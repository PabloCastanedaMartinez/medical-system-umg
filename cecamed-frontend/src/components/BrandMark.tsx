/** Marca de CECAMed: la cruz médica del sistema original, dibujada a mano para no depender de un ícono genérico. */
export function BrandMark({ size = 26 }: { size?: number }) {
  return (
    <svg className="brand-mark" width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="6" fill="var(--brand)" />
      <path d="M13 7h6v6h6v6h-6v6h-6v-6H7v-6h6z" fill="#fff" />
    </svg>
  );
}
