import type { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: number | string | null;
  subtitle: string;
  icon: LucideIcon;
  variant: 'primary' | 'warning' | 'success' | 'accent';
}

export function StatCard({ title, value, subtitle, icon: Icon, variant }: StatCardProps) {
  return (
    <div className={`card stat-card stat-${variant}`}>
      <div className="stat-text">
        <span className="stat-title">{title}</span>
        <span className="stat-value">{value ?? '—'}</span>
        <span className="stat-subtitle">{subtitle}</span>
      </div>
      <div className="stat-icon" aria-hidden>
        <Icon size={22} />
      </div>
    </div>
  );
}
