import { STATUS_LABELS } from './farmerStatus'

const TONES = {
  ACTIVE: 'bg-brand-50 text-brand-700 ring-brand-200',
  SUSPENDED: 'bg-accent-500/15 text-warning ring-accent-500/40',
  WITHDRAWN: 'bg-surface text-muted ring-line',
  RETIRED: 'bg-surface text-ink ring-line',
}

/** A farmer's status as a coloured label; the text is always shown, so colour is never the only signal (NFR-08). */
export function StatusBadge({ status }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${TONES[status] ?? TONES.WITHDRAWN}`}
    >
      {STATUS_LABELS[status] ?? status}
    </span>
  )
}
