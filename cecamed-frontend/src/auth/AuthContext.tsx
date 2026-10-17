import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { UNAUTHORIZED_EVENT } from '../api/client';
import type { SessionUser } from '../api/types';

interface AuthContextValue {
  user: SessionUser | null;
  /** true mientras se comprueba si ya existe una sesión en el servidor. */
  checking: boolean;
  /** true cuando la sesión se cerró por expiración (para avisar en la pantalla de acceso). */
  expired: boolean;
  isMedico: boolean;
  isRecepcion: boolean;
  login: (username: string, password: string) => Promise<SessionUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Sustituye a UserSession/AuthenticationService de JavaFX: la sesión es un token Bearer guardado en la pestaña. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null);
  const [checking, setChecking] = useState(true);
  const [expired, setExpired] = useState(false);

  useEffect(() => {
    let cancelled = false;
    authApi
      .me()
      .then((u) => !cancelled && setUser(u))
      .catch(() => !cancelled && setUser(null))
      .finally(() => !cancelled && setChecking(false));
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const onUnauthorized = () => {
      setUser((current) => {
        if (current) setExpired(true);
        return null;
      });
    };
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const u = await authApi.login(username, password);
    setExpired(false);
    setUser(u);
    return u;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setExpired(false);
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      checking,
      expired,
      isMedico: user?.role === 'MEDICO',
      isRecepcion: user?.role === 'RECEPCION',
      login,
      logout,
    }),
    [user, checking, expired, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return ctx;
}
