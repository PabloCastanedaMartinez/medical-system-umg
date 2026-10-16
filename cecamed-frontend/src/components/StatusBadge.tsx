import {
  AlertTriangle,
  Calendar,
  Check,
  CheckCircle2,
  Clock,
  Cloud,
  CloudOff,
  Loader,
  RefreshCw,
  UserX,
  XCircle,
  type LucideIcon,
} from 'lucide-react';
import type { AppointmentStatus, GoogleSyncStatus } from '../api/types';
import { APPOINTMENT_STATUS, GOOGLE_SYNC, type Tone } from '../utils/labels';

const STATUS_ICON: Record<AppointmentStatus, LucideIcon> = {
  CONFIRMADA: CheckCircle2,
  EN_SALA: Clock,
  ATENDIDA: Check,
  PROGRAMADA: Calendar,
  REPROGRAMADA: RefreshCw,
  NO_ASISTIO: UserX,
  CANCELADA: XCircle,
};

const SYNC_ICON: Record<GoogleSyncStatus, LucideIcon> = {
  SYNCED: Cloud,
  PENDING: Loader,
  FAILED: AlertTriangle,
  NOT_APPLICABLE: CloudOff,
};

export function Badge({ tone, children, icon: Icon }: { tone: Tone; children: React.ReactNode; icon?: LucideIcon }) {
  return (
    <span className={`badge badge-${tone}`}>
      {Icon && <Icon size={12} aria-hidden />}
      {children}
    </span>
  );
}

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus | null | undefined }) {
  if (!status) return <Badge tone="neutral">N/D</Badge>;
  const meta = APPOINTMENT_STATUS[status];
  return (
    <Badge tone={meta.tone} icon={STATUS_ICON[status]}>
      {meta.label}
    </Badge>
  );
}

export function SyncStatusBadge({ status }: { status: GoogleSyncStatus | null | undefined }) {
  if (!status) return <Badge tone="neutral">Sin Sync</Badge>;
  const meta = GOOGLE_SYNC[status];
  return (
    <Badge tone={meta.tone} icon={SYNC_ICON[status]}>
      {meta.label}
    </Badge>
  );
}
