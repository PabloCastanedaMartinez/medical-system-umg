import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import type { AppointmentStatus, GoogleSyncStatus } from '../api/types';
import { APPOINTMENT_STATUS, GOOGLE_SYNC, type Tone } from '../utils/labels';

/** Estado con un punto de color y texto: se lee en tablas densas sin competir con el contenido. */
export function Badge({ tone, children }: { tone: Tone; children: ReactNode; icon?: LucideIcon }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus | null | undefined }) {
  if (!status) return <Badge tone="neutral">Sin estado</Badge>;
  const meta = APPOINTMENT_STATUS[status];
  return <Badge tone={meta.tone}>{meta.label}</Badge>;
}

export function SyncStatusBadge({ status }: { status: GoogleSyncStatus | null | undefined }) {
  if (!status) return <Badge tone="neutral">Sin sincronizar</Badge>;
  const meta = GOOGLE_SYNC[status];
  return <Badge tone={meta.tone}>{meta.label}</Badge>;
}
