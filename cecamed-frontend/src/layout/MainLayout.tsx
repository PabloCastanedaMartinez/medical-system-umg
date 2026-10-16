import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router';
import {
  CalendarDays,
  ClipboardList,
  Cross,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Settings,
  Sun,
  UserCheck,
  Users,
  X,
  type LucideIcon,
} from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../theme/ThemeContext';
import { useNow, useApi } from '../utils/useApi';
import { formatClock } from '../utils/format';
import { systemApi } from '../api/endpoints';
import { SyncStatusBadge } from '../components/StatusBadge';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  medicoOnly?: boolean;
  end?: boolean;
}

const CLINICAL: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/pacientes', label: 'Pacientes', icon: Users },
  { to: '/expediente', label: 'Expediente Clínico', icon: ClipboardList, medicoOnly: true },
  { to: '/agenda', label: 'Agenda Médica', icon: CalendarDays },
  { to: '/recepcion', label: 'Recepción & Espera', icon: UserCheck },
];

const ADMIN: NavItem[] = [{ to: '/configuracion', label: 'Configuración', icon: Settings }];

const TITLES: Record<string, string> = {
  '/': 'Panel Principal',
  '/pacientes': 'Directorio de Pacientes',
  '/expediente': 'Expediente Clínico',
  '/agenda': 'Agenda y Citas Médicas',
  '/recepcion': 'Recepción y Sala de Espera',
  '/configuracion': 'Configuración del Sistema',
};

/** Equivalente de main-layout.fxml / MainLayoutController. */
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
    document.title = `${TITLES[section] ?? 'CECAMed'} - CECAMed`;
  }, [section]);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const renderItem = (item: NavItem) =>
    item.medicoOnly && !isMedico ? null : (
      <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}>
        <item.icon size={18} aria-hidden />
        <span>{item.label}</span>
      </NavLink>
    );

  return (
    <div className={`app-shell${menuOpen ? ' menu-open' : ''}`}>
      <aside className="sidebar" aria-label="Navegación principal">
        <div className="brand">
          <div className="brand-mark" aria-hidden>
            <Cross size={20} />
          </div>
          <div>
            <div className="brand-title">CECAMed</div>
            <div className="brand-subtitle">Control &amp; Gestión Médica</div>
          </div>
          <button className="icon-button sidebar-close" onClick={() => setMenuOpen(false)} aria-label="Cerrar menú">
            <X size={18} />
          </button>
        </div>
        <nav>
          <div className="nav-section">OPERACIÓN CLÍNICA</div>
          {CLINICAL.map(renderItem)}
          <div className="nav-section">ADMINISTRACIÓN</div>
          {ADMIN.map(renderItem)}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="avatar" aria-hidden>
              {user?.fullName
                .replace(/^(Dr|Dra|Lic|Licda)\.\s*/i, '')
                .split(' ')
                .slice(0, 2)
                .map((p) => p[0])
                .join('')}
            </div>
            <div className="user-meta">
              <strong title={user?.fullName}>{user?.fullName}</strong>
              <span>{user?.roleDisplayName}</span>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={handleLogout}>
            <LogOut size={16} aria-hidden /> Salir
          </button>
        </div>
      </aside>
      <div className="sidebar-scrim" onClick={() => setMenuOpen(false)} aria-hidden />

      <div className="main-column">
        <header className="topbar">
          <button className="icon-button menu-toggle" onClick={() => setMenuOpen(true)} aria-label="Abrir menú">
            <Menu size={20} />
          </button>
          <div className="clock" aria-live="off">
            {formatClock(now)}
          </div>
          <div className="topbar-actions">
            {import.meta.env.VITE_USE_MOCK === 'true' && (
              <span className="badge badge-warning" title="Los datos son de ejemplo y se reinician al recargar">
                API simulada
              </span>
            )}
            <span className="sync-indicator" title="Google Calendar">
              <span className="muted hide-sm">Google Calendar:</span>
              <SyncStatusBadge status={systemInfo ? (systemInfo.googleCalendarEnabled ? 'SYNCED' : 'NOT_APPLICABLE') : 'NOT_APPLICABLE'} />
            </span>
            <button
              className="icon-button"
              onClick={toggleTheme}
              aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
              title={isDark ? 'Modo claro' : 'Modo oscuro'}
            >
              {isDark ? <Sun size={18} /> : <Moon size={18} />}
            </button>
          </div>
        </header>
        <main className="content" id="contenido">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
