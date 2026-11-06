const DATE = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })

/** "2026-11-06" or an ISO instant → "6 Nov 2026". A plain date is read as that calendar day, not as UTC midnight. */
export function formatDate(value) {
  if (!value) return '—'
  const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value)
  return Number.isNaN(date.getTime()) ? value : DATE.format(date)
}

/** Today's date on this computer as "YYYY-MM-DD", for date inputs that may not be in the future. */
export function todayIso() {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
