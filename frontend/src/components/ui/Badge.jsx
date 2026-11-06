const TONES = {
  success: 'bg-brand-50 text-brand-700 ring-brand-200',
  warning: 'bg-accent-500/15 text-warning ring-accent-500/40',
  danger: 'bg-danger/5 text-danger ring-danger/30',
  neutral: 'bg-surface text-muted ring-line',
}

/** A short coloured label. The text always says what the colour means (URD NFR-08). */
export function Badge({ tone = 'neutral', children }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap ring-1 ring-inset ${TONES[tone]}`}
    >
      {children}
    </span>
  )
}
