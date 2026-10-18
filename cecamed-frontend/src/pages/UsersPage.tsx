import { useState, type FormEvent } from 'react';
import { usersApi } from '../api/endpoints';
import type { UserRole } from '../api/types';
import { useApi } from '../utils/useApi';
import { errorMessage } from '../api/client';
import { PageHeader, Field, Spinner, ErrorBanner } from '../components/Common';
import { useToast } from '../components/Toast';
export function UsersPage() {
  const query = useApi(() => usersApi.list(), []); const toast = useToast();
  const [form, setForm] = useState({ username: '', password: '', fullName: '', role: 'RECEPCION' as UserRole });
  const [saving, setSaving] = useState(false); const [error, setError] = useState<string | null>(null);
  const save = async (e: FormEvent) => { e.preventDefault(); setSaving(true); setError(null);
    try { await usersApi.create(form); setForm({ username: '', password: '', fullName: '', role: 'RECEPCION' }); query.reload(); toast.success('Cuenta creada', 'El usuario ya puede iniciar sesión.'); }
    catch (err) { setError(errorMessage(err)); } finally { setSaving(false); }
  };
  return <div className="page"><PageHeader title="Usuarios" subtitle="Accesos del personal del consultorio" />
    <section className="card"><h2>Registrar cuenta</h2><form onSubmit={save}><fieldset className="form-section" disabled={saving}><div className="form-grid">
      <Field label="Nombre completo" htmlFor="user-name" required><input id="user-name" required maxLength={150} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></Field>
      <Field label="Usuario" htmlFor="user-login" required><input id="user-login" required minLength={3} maxLength={80} value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></Field>
      <Field label="Contraseña" htmlFor="user-password" required><input id="user-password" type="password" autoComplete="new-password" required minLength={12} maxLength={100} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field>
      <Field label="Rol" htmlFor="user-role"><select id="user-role" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as UserRole })}><option value="RECEPCION">Recepción</option><option value="MEDICO">Médico</option><option value="ADMIN">Administrador</option></select></Field>
    </div><button className="btn btn-primary" type="submit">{saving && <Spinner size={16} />} Crear cuenta</button></fieldset></form>{error && <div className="alert alert-danger" role="alert">{error}</div>}</section>
    <section className="card"><h2>Cuentas registradas</h2>{query.error && <ErrorBanner message={query.error} onRetry={query.reload} />}{query.loading && <Spinner />}
      <div className="table-wrap"><table className="table"><thead><tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th>Acción</th></tr></thead><tbody>{query.data?.content.map((u) => <tr key={u.id}><td>{u.username}</td><td>{u.fullName}</td><td>{u.role}</td><td>{u.active ? 'Activo' : 'Inactivo'}</td><td><button className="btn btn-ghost btn-sm" onClick={async () => { try { await usersApi.setActive(u); query.reload(); } catch (err) { toast.error('No se pudo actualizar', errorMessage(err)); } }}>{u.active ? 'Desactivar' : 'Activar'}</button></td></tr>)}</tbody></table></div>
    </section></div>;
}
