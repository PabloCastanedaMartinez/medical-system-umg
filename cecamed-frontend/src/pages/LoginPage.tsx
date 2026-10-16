import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { ClipboardList, Cross, Eye, EyeOff, LogIn, Moon, Stethoscope, Sun } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../theme/ThemeContext';
import { useToast } from '../components/Toast';
import { Spinner } from '../components/Common';
import { errorMessage } from '../api/client';

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
    return <Navigate to={from} replace />;
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
      toast.success('Bienvenido a CECAMed', `Sesión iniciada como ${session.fullName} (${session.roleDisplayName})`);
      navigate(from, { replace: true });
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
      <button
        className="icon-button login-theme"
        onClick={toggleTheme}
        aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
      >
        {isDark ? <Sun size={18} /> : <Moon size={18} />}
      </button>

      <section className="login-hero" aria-hidden>
        <div className="login-hero-inner">
          <div className="brand-mark brand-mark-lg">
            <Cross size={30} />
          </div>
          <h2>CECAMed</h2>
          <p>Sistema de Control y Gestión Médica</p>
          <ul>
            <li>Pacientes y expedientes clínicos centralizados</li>
            <li>Agenda, recepción y sala de espera en tiempo real</li>
            <li>Consultas, signos vitales y documentos adjuntos</li>
          </ul>
        </div>
      </section>

      <section className="login-panel">
        <form className="login-card" onSubmit={onSubmit} noValidate>
          <div className="login-card-head">
            <div className="brand-mark" aria-hidden>
              <Cross size={20} />
            </div>
            <div>
              <h1>¡Bienvenido a CECAMed!</h1>
              <p className="muted">Ingrese sus credenciales para continuar</p>
            </div>
          </div>

          {expired && !error && (
            <div className="alert alert-warning">Su sesión expiró. Inicie sesión nuevamente.</div>
          )}

          <div className="field">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              autoComplete="username"
              placeholder="Su usuario"
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
                placeholder="••••••••"
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
            {loading ? <Spinner size={16} /> : <LogIn size={18} aria-hidden />}
            Iniciar Sesión
          </button>

          {import.meta.env.VITE_USE_MOCK === 'true' && (
          <div className="quick-access">
            <span className="muted">Acceso rápido con la API simulada:</span>
            <div className="quick-access-buttons">
              <button type="button" className="btn btn-outline" disabled={loading} onClick={() => quickFill('medico', 'medico123')}>
                <Stethoscope size={16} aria-hidden /> Rol: Médico
              </button>
              <button
                type="button"
                className="btn btn-outline"
                disabled={loading}
                onClick={() => quickFill('recepcion', 'recepcion123')}
              >
                <ClipboardList size={16} aria-hidden /> Rol: Recepción
              </button>
            </div>
          </div>
          )}
        </form>
        <p className="login-footer muted">CECAMed Clínico • React + Spring Boot</p>
      </section>
    </div>
  );
}
