import { Link } from 'react-router'
import { Alert } from '../../components/ui/Alert'
import { Spinner } from '../../components/ui/Spinner'

/**
 * One dashboard card. It waits for its own request and shows its own error, so one failing request never
 * hides the other cards (URD §17). `children` is a function of the loaded data when `query` is given.
 * @param {{ title: string, query?: { isPending: boolean, error: Error | null, data: any },
 *   to?: string, linkLabel?: string, wide?: boolean, children: any }} props
 */
export function DashboardCard({ title, query, to, linkLabel, wide = false, children }) {
  let body
  if (query?.isPending) {
    body = (
      <div className="py-4 text-brand-700">
        <Spinner label={`Loading ${title.toLowerCase()}`} />
      </div>
    )
  } else if (query?.error) {
    body = <Alert tone="error">{query.error.message}</Alert>
  } else {
    body = typeof children === 'function' ? children(query?.data) : children
  }
  return (
    <section
      aria-label={title}
      className={`flex flex-col rounded-lg border border-line bg-white p-5 ${wide ? 'lg:col-span-2' : ''}`}
    >
      <h2 className="mb-3 text-sm font-semibold tracking-wide text-muted uppercase">{title}</h2>
      <div className="flex-1">{body}</div>
      {to && (
        <Link to={to} className="mt-4 self-start text-sm font-semibold text-brand-700 hover:underline">
          {linkLabel} →
        </Link>
      )}
    </section>
  )
}

/** A headline number with its label. */
export function Figure({ label, value, sub }) {
  return (
    <div>
      <p className="text-2xl font-semibold text-ink">{value}</p>
      <p className="text-xs font-medium text-muted">{label}</p>
      {sub && <p className="text-xs text-muted">{sub}</p>}
    </div>
  )
}

export function Figures({ children }) {
  return <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">{children}</div>
}

/** A short list of rows inside a card; each row is { key, main, side? , to? }. */
export function CardList({ rows, empty }) {
  if (!rows.length) return <p className="text-sm text-muted">{empty}</p>
  return (
    <ul className="divide-y divide-line text-sm">
      {rows.map((row) => (
        <li key={row.key} className="flex items-center justify-between gap-3 py-2">
          <span className="min-w-0">
            {row.to ? (
              <Link to={row.to} className="font-medium text-brand-700 hover:underline">
                {row.main}
              </Link>
            ) : (
              <span className="font-medium text-ink">{row.main}</span>
            )}
            {row.detail && <span className="block text-xs text-muted">{row.detail}</span>}
          </span>
          {row.side != null && <span className="shrink-0 text-right">{row.side}</span>}
        </li>
      ))}
    </ul>
  )
}
