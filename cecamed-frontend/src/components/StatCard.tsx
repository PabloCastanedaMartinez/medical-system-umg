import type { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: number | string | null;
  subtitle: string;
  /** Se conserva por compatibilidad; el resumen ya no usa íconos decorativos. */
  icon?: LucideIcon;
  /** "warning" resalta la cifra cuando es mayor que cero (p. ej. pacientes esperando). */
  variant: 'primary' | 'warning' | 'success' | 'accent';
}

/** Una cifra del resumen del día. Se agrupan dentro de `.stat-grid`, que las muestra como una sola franja. */
export function StatCard({ title, value, subtitle, variant }: StatCardProps) {
  const attention = variant === 'warning' && typeof value === 'number' && value > 0;
  return (
    <div className={`stat${attention ? ' stat-attention' : ''}`}>
      <span className="stat-title">{title}</span>
      <span className="stat-value">{value ?? '–'}</span>
      <span className="stat-subtitle">{subtitle}</span>
    </div>
  );
}
