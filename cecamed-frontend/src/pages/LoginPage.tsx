import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { Eye, EyeOff, Moon, Sun } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../theme/ThemeContext';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Common';
import { BrandMark } from '../components/BrandMark';
import type { UserRole } from '../api/types';
import { errorMessage } from '../api/client';

function destination(role: UserRole, requested: string): string {
  const section = '/' + requested.split('/')[1]?.split('?')[0];
  const allowed: Record<UserRole, string[]> = {
    PACIENTE: ['/mis-citas', '/configuracion'],
    MEDICO: ['/', '/agenda', '/pacientes', '/expediente', '/configuracion'],
    RECEPCION: ['/', '/agenda', '/recepcion', '/pacientes', '/medicos', '/configuracion'],
    ADMIN: ['/', '/agenda', '/recepcion', '/pacientes', '/medicos', '/usuarios', '/configuracion'],
  };
  return allowed[role].includes(section) ? requested : role === 'PACIENTE' ? '/mis-citas' : '/';
}

/** Equivalente de login.fxml / LoginController. */
export function LoginPage() {
  const { user, login, expired, checking } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!checking && user) {
    return <Navigate to={destination(user.role, from)} replace />;
  }

  const doLogin = async (u: string, p: string) => {
    if (!u.trim()) {
      setError('Por favor ingrese su usuario');
      return;
    }
    if (!p.trim()) {
      setError('Por favor ingrese su contraseña');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const session = await login(u, p);
      toast.success('Sesión iniciada', `${session.fullName}, ${session.roleDisplayName.toLowerCase()}`);
      navigate(destination(session.role, from), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    void doLogin(username, password);
  };

  const quickFill = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
    void doLogin(u, p);
  };

  return (
    <div className="login-page">
      <header className="login-bar">
        <span className="brand">
          <BrandMark />
          <span className="brand-name">CECAMed</span>
        </span>
        <button
          className="appbar-icon"
          onClick={toggleTheme}
          aria-label={isDark ? 'Usar tema claro' : 'Usar tema oscuro'}
          title={isDark ? 'Tema claro' : 'Tema oscuro'}
        >
          {isDark ? <Sun size={17} /> : <Moon size={17} />}
        </button>
      </header>

      <main className="login-main">
        <form className="login-card" onSubmit={onSubmit} noValidate>
          <h1>Iniciar sesión</h1>
          <p className="muted login-lead">Sistema de control y gestión médica</p>

          {expired && !error && (
            <div className="alert alert-warning">Su sesión expiró. Vuelva a iniciar sesión.</div>
          )}

          <div className="field">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoFocus
            />
          </div>

          <div className="field">
            <label htmlFor="password">Contraseña</label>
            <div className="input-with-button">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
              />
              <button
                type="button"
                className="icon-button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {error && (
            <div className="alert alert-danger" role="alert">
              {error}
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
            {loading && <Spinner size={16} />}
            Entrar
          </button>

          {import.meta.env.VITE_USE_MOCK === 'true' && (
            <div className="quick-access">
              <p className="muted">Usuarios de prueba (API simulada)</p>
              <div className="quick-access-buttons">
                <button type="button" className="btn btn-secondary" disabled={loading} onClick={() => quickFill('medico', 'medico123')}>
                  Entrar como médico
                </button>
                <button type="button" className="btn btn-secondary" disabled={loading} onClick={() => quickFill('recepcion', 'recepcion123')}>
                  Entrar como recepción
                </button>
              </div>
            </div>
          )}
        </form>
      </main>
    </div>
  );
}
