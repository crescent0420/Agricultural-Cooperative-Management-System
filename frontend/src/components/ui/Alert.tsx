import type { ReactNode } from 'react'

type Tone = 'info' | 'success' | 'warning' | 'error'

const TONES: Record<Tone, string> = {
  info: 'border-brand-200 bg-brand-50 text-ink',
  success: 'border-brand-200 bg-brand-50 text-brand-700',
  warning: 'border-accent-500 bg-accent-500/10 text-ink',
  error: 'border-danger/40 bg-danger/5 text-danger',
}

/** A message box. Errors and warnings are announced to screen readers immediately. */
export function Alert({ tone = 'info', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <div
      role={tone === 'error' || tone === 'warning' ? 'alert' : 'status'}
      className={`rounded-lg border px-4 py-3 text-sm ${TONES[tone]}`}
    >
      {children}
    </div>
  )
}
