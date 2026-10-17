import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from './AuthContext';
import type { UserRole } from '../api/types';
import { EmptyState, Spinner } from '../components/Common';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, checking } = useAuth();
  const location = useLocation();

  if (checking) {
    return (
      <div className="fullscreen-center">
        <Spinner size={28} label="Comprobando sesión" />
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  return <>{children}</>;
}

/** Restringe una vista a ciertos roles (p. ej. el expediente clínico solo para MEDICO). */
export function RequireRole({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) {
    return (
      <EmptyState
        icon={ShieldAlert}
        message="Acceso restringido: el perfil de Recepción no tiene permisos para consultar el expediente clínico detallado."
      />
    );
  }
  return <>{children}</>;
}
