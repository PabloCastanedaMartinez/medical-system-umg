import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';
import { Modal } from './Modal';

interface ConfirmOptions {
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: 'primary' | 'danger' | 'accent';
}

interface PromptOptions extends ConfirmOptions {
  inputLabel: string;
  placeholder?: string;
  required?: boolean;
}

interface ConfirmApi {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
  /** Pide un texto (p. ej. motivo de cancelación). Devuelve null si se cancela. */
  prompt: (options: PromptOptions) => Promise<string | null>;
}

const ConfirmContext = createContext<ConfirmApi | null>(null);

type Pending =
  | { kind: 'confirm'; options: ConfirmOptions; resolve: (v: boolean) => void }
  | { kind: 'prompt'; options: PromptOptions; resolve: (v: string | null) => void };

/** Sustituye ConfirmationDialog y TextInputDialog de JavaFX. */
export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [pending, setPending] = useState<Pending | null>(null);
  const [text, setText] = useState('');
  const [touched, setTouched] = useState(false);
  const pendingRef = useRef<Pending | null>(null);
  pendingRef.current = pending;

  const confirm = useCallback(
    (options: ConfirmOptions) =>
      new Promise<boolean>((resolve) => setPending({ kind: 'confirm', options, resolve })),
    [],
  );

  const prompt = useCallback((options: PromptOptions) => {
    setText('');
    setTouched(false);
    return new Promise<string | null>((resolve) => setPending({ kind: 'prompt', options, resolve }));
  }, []);

  const close = (accepted: boolean) => {
    const p = pendingRef.current;
    if (!p) return;
    if (p.kind === 'confirm') {
      p.resolve(accepted);
    } else {
      if (accepted && p.options.required !== false && !text.trim()) {
        setTouched(true);
        return;
      }
      p.resolve(accepted ? text.trim() : null);
    }
    setPending(null);
  };

  const options = pending?.options;
  const tone = options?.tone ?? 'primary';
  const showError = pending?.kind === 'prompt' && touched && !text.trim();

  return (
    <ConfirmContext.Provider value={{ confirm, prompt }}>
      {children}
      <Modal
        open={pending !== null}
        title={options?.title ?? ''}
        onClose={() => close(false)}
        size="sm"
        footer={
          <>
            <button className="btn btn-ghost" onClick={() => close(false)}>
              {options?.cancelLabel ?? 'Cancelar'}
            </button>
            <button className={`btn btn-${tone}`} onClick={() => close(true)}>
              {options?.confirmLabel ?? 'Aceptar'}
            </button>
          </>
        }
      >
        <div className="confirm-message">{options?.message}</div>
        {pending?.kind === 'prompt' && (
          <form
            className="field"
            onSubmit={(e) => {
              e.preventDefault();
              close(true);
            }}
          >
            <label htmlFor="confirm-prompt-input">
              {pending.options.inputLabel}
              {pending.options.required !== false && ' *'}
            </label>
            <input
              id="confirm-prompt-input"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder={pending.options.placeholder}
              maxLength={255}
              aria-invalid={showError}
            />
            {showError && <span className="field-error">Este campo es obligatorio</span>}
          </form>
        )}
      </Modal>
    </ConfirmContext.Provider>
  );
}

export function useConfirm(): ConfirmApi {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useConfirm debe usarse dentro de <ConfirmProvider>');
  return ctx;
}
