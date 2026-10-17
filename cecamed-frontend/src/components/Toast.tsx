import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';
import { AlertTriangle, CheckCircle2, Info, X, XCircle } from 'lucide-react';

type ToastKind = 'success' | 'warning' | 'error' | 'info';

interface ToastItem {
  id: number;
  kind: ToastKind;
  title: string;
  message: string;
}

interface ToastApi {
  success: (title: string, message: string) => void;
  warning: (title: string, message: string) => void;
  error: (title: string, message: string) => void;
  info: (title: string, message: string) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

const DURATION: Record<ToastKind, number> = { success: 4000, info: 4000, warning: 5000, error: 6000 };
const ICON = { success: CheckCircle2, warning: AlertTriangle, error: XCircle, info: Info };

/** Equivalente web de NotificationService (ControlsFX Notifications). */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => setToasts((list) => list.filter((t) => t.id !== id)), []);

  const push = useCallback(
    (kind: ToastKind, title: string, message: string) => {
      const id = nextId.current++;
      setToasts((list) => [...list.slice(-2), { id, kind, title, message }]);
      window.setTimeout(() => dismiss(id), DURATION[kind]);
    },
    [dismiss],
  );

  const api = useMemo<ToastApi>(
    () => ({
      success: (t, m) => push('success', t, m),
      warning: (t, m) => push('warning', t, m),
      error: (t, m) => push('error', t, m),
      info: (t, m) => push('info', t, m),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-region" role="region" aria-live="polite" aria-label="Notificaciones">
        {toasts.map((t) => {
          const Icon = ICON[t.kind];
          return (
            <div key={t.id} className={`toast toast-${t.kind}`} role={t.kind === 'error' ? 'alert' : 'status'}>
              <Icon size={18} className="toast-icon" aria-hidden />
              <div className="toast-body">
                <strong>{t.title}</strong>
                <span>{t.message}</span>
              </div>
              <button className="icon-button" onClick={() => dismiss(t.id)} aria-label="Cerrar notificación">
                <X size={14} />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast debe usarse dentro de <ToastProvider>');
  return ctx;
}
