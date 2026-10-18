import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';

let client;
beforeEach(async () => {
  const values = new Map();
  globalThis.sessionStorage = { getItem: (k) => values.get(k) ?? null, setItem: (k, v) => values.set(k, v), removeItem: (k) => values.delete(k) };
  globalThis.window = new EventTarget();
  const source = (await readFile(new URL('../src/api/client.ts', import.meta.url), 'utf8')).replaceAll('import.meta.env', '({})');
  const js = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ES2022 } }).outputText;
  client = await import(`data:text/javascript;base64,${Buffer.from(js).toString('base64')}#${Math.random()}`);
  client.setSession({ accessToken: 'expired', refreshToken: 'refresh-1' });
});
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

test('parallel 401 responses share one refresh and retry with the rotated token', async () => {
  let refreshes = 0;
  let release;
  const barrier = new Promise((resolve) => { release = resolve; });
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/auth/refresh')) {
      refreshes++;
      assert.equal(options.headers.Authorization, undefined);
      assert.deepEqual(JSON.parse(options.body), { refreshToken: 'refresh-1' });
      await barrier;
      return json({ accessToken: 'new', refreshToken: 'refresh-2' });
    }
    return options.headers.Authorization === 'Bearer new' ? json({ ok: true }) : json({}, 401);
  };
  const requests = [client.request('/patients'), client.request('/auth/me', { silentAuth: true })];
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(refreshes, 1);
  release();
  assert.deepEqual(await Promise.all(requests), [{ ok: true }, { ok: true }]);
  assert.equal(sessionStorage.getItem('cecamed.refreshToken'), 'refresh-2');
});

test('a rejected refresh clears both tokens and reports session expiration', async () => {
  let expirations = 0;
  window.addEventListener(client.UNAUTHORIZED_EVENT, () => expirations++);
  globalThis.fetch = async () => json({}, 401);
  await assert.rejects(client.request('/patients'), (error) => error.status === 401);
  assert.equal(client.getAccessToken(), null);
  assert.equal(sessionStorage.getItem('cecamed.refreshToken'), null);
  assert.equal(expirations, 1);
});

test('logout waits for rotation and returns the latest refresh token for revocation', async () => {
  let release;
  const barrier = new Promise((resolve) => { release = resolve; });
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/auth/refresh')) {
      await barrier;
      return json({ accessToken: 'new', refreshToken: 'refresh-2' });
    }
    return options.headers.Authorization === 'Bearer new' ? json({}) : json({}, 401);
  };
  const request = client.request('/patients').catch(() => undefined);
  await new Promise((resolve) => setImmediate(resolve));
  const logout = client.endSession();
  release();
  assert.equal(await logout, 'refresh-2');
  await request;
  assert.equal(client.getAccessToken(), null);
  assert.equal(sessionStorage.getItem('cecamed.refreshToken'), null);
});

test('network failures do not discard a refresh token or replay writes', async () => {
  let calls = 0;
  globalThis.fetch = async () => { calls++; throw new TypeError('offline'); };
  await assert.rejects(client.request('/patients', { method: 'POST', body: {} }), (error) => error.status === 0);
  assert.equal(calls, 1);
  assert.equal(sessionStorage.getItem('cecamed.refreshToken'), 'refresh-1');
});
