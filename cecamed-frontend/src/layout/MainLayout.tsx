import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router';
import { LogOut, Menu, Moon, Sun, X } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../theme/ThemeContext';
import { useNow, useApi } from '../utils/useApi';
import { formatClock } from '../utils/format';
import { systemApi } from '../api/endpoints';
import { BrandMark } from '../components/BrandMark';

interface NavItem {
  to: string;
  label: string;
  medicoOnly?: boolean;
  end?: boolean;
}

// Orden de trabajo de la clínica: el día, los pacientes, la agenda y la sala de espera.
const NAV: NavItem[] = [
  { to: '/', label: 'Inicio', end: true },
  { to: '/pacientes', label: 'Pacientes' },
  { to: '/expediente', label: 'Expediente clínico', medicoOnly: true },
  { to: '/agenda', label: 'Agenda' },
  { to: '/recepcion', label: 'Recepción' },
  { to: '/configuracion', label: 'Configuración' },
];

const TITLES: Record<string, string> = {
  '/': 'Inicio',
  '/pacientes': 'Pacientes',
  '/expediente': 'Expediente clínico',
  '/agenda': 'Agenda',
  '/recepcion': 'Recepción',
  '/configuracion': 'Configuración',
};

/** Estructura principal: barra superior con la navegación (equivale a main-layout.fxml). */
export function MainLayout() {
  const { user, isMedico, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const now = useNow(1000);
  const [menuOpen, setMenuOpen] = useState(false);
  const { data: systemInfo } = useApi(() => systemApi.info(), []);

  useEffect(() => setMenuOpen(false), [location.pathname]);

  const section = '/' + (location.pathname.split('/')[1] ?? '');
  useEffect(() => {
    document.title = `${TITLES[section] ?? 'CECAMed'} · CECAMed`;
  }, [section]);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const items = NAV.filter((item) => (!item.medicoOnly || isMedico) && (user?.role !== 'ADMIN' || item.to === '/configuracion'));

  return (
    <div className="app-shell">
      <header className="appbar">
        <div className="appbar-inner">
          <NavLink to="/" className="brand" aria-label="CECAMed, ir a inicio">
            <BrandMark />
            <span className="brand-name">CECAMed</span>
          </NavLink>

          <nav className={`mainnav${menuOpen ? ' open' : ''}`} aria-label="Navegación principal" id="mainnav">
            {items.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => `mainnav-link${isActive ? ' active' : ''}`}>
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="appbar-tools">
            <button
              className="appbar-icon"
              onClick={toggleTheme}
              aria-label={isDark ? 'Usar tema claro' : 'Usar tema oscuro'}
              title={isDark ? 'Tema claro' : 'Tema oscuro'}
            >
              {isDark ? <Sun size={18} /> : <Moon size={18} />}
            </button>
            <div className="appbar-session">
              <div className="appbar-user" title={user?.fullName}>
                <span className="appbar-user-name">{user?.fullName}</span>
                <span className="appbar-user-role">{user?.roleDisplayName}</span>
              </div>
              <button className="appbar-icon" onClick={handleLogout} aria-label="Cerrar sesión" title="Cerrar sesión">
                <LogOut size={18} />
              </button>
            </div>
            <button
              className="appbar-icon menu-toggle"
              onClick={() => setMenuOpen((v) => !v)}
              aria-expanded={menuOpen}
              aria-controls="mainnav"
              aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
            >
              {menuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>
      </header>

      <div className="statusline">
        <div className="statusline-inner">
          <span className="clock">{formatClock(now)}</span>
          <span className="statusline-item">
            Google Calendar: {systemInfo?.googleCalendarEnabled ? 'sincronizado' : 'sin conectar'}
          </span>
          {import.meta.env.VITE_USE_MOCK === 'true' && (
            <span className="statusline-item statusline-warn" title="Los datos son de ejemplo y se reinician al recargar">
              Datos de ejemplo (API simulada)
            </span>
          )}
        </div>
      </div>

      <main className="content" id="contenido">
        <Outlet />
      </main>
    </div>
  );
}
