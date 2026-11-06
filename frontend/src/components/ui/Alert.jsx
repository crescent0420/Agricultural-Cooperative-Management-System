const TONES = {
  info: 'border-brand-200 bg-brand-50 text-ink',
  success: 'border-brand-200 bg-brand-50 text-brand-700',
  warning: 'border-accent-500 bg-accent-500/10 text-warning',
  error: 'border-danger/40 bg-danger/5 text-danger',
}

const SIGNS = { warning: '⚠', error: '✕' }

/**
 * A message box. Warnings are yellow (amber text, yellow border and ⚠), errors red (✕), as for form fields.
 * Errors and warnings are announced to screen readers immediately.
 */
export function Alert({ tone = 'info', children }) {
  return (
    <div
      role={tone === 'error' || tone === 'warning' ? 'alert' : 'status'}
      className={`flex items-start gap-2 rounded-lg border px-4 py-3 text-sm ${TONES[tone]}`}
    >
      {SIGNS[tone] && (
        <span aria-hidden="true" className={tone === 'warning' ? 'text-accent-500' : undefined}>
          {SIGNS[tone]}
        </span>
      )}
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}
