import { Alert } from '../../components/ui/Alert'
import { Spinner } from '../../components/ui/Spinner'
import { formatDateTime } from '../../utils/format'

/** Headline numbers, taken from the report's totals (never added up here). */
export function StatTiles({ tiles }) {
  return (
    <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {tiles.map((t) => (
        <div key={t.label} className="rounded-lg border border-line bg-white px-4 py-3">
          <dt className="text-xs font-medium text-muted">{t.label}</dt>
          <dd className="mt-1 text-xl font-semibold text-ink">{t.value}</dd>
          {t.sub && <dd className="text-xs text-muted">{t.sub}</dd>}
        </div>
      ))}
    </dl>
  )
}

/**
 * The full report as a table with the backend's totals row in the footer. No sorting or paging: the backend
 * returns the whole report in its own order (FR-REP).
 * @param {{ caption: string, rows: object[], rowKey: (row: object) => string | number, totals: object,
 *   columns: { header: string, cell: (row: object) => import('react').ReactNode,
 *     total?: (totals: object) => import('react').ReactNode, numeric?: boolean }[] }} props
 */
export function ReportTable({ caption, columns, rows, rowKey, totals }) {
  const align = (c) => (c.numeric ? 'text-right tabular-nums' : '')
  return (
    <div className="overflow-x-auto rounded-lg border border-line bg-white">
      <table className="min-w-full text-left text-sm">
        <caption className="sr-only">{caption}</caption>
        <thead className="border-b border-line bg-surface text-xs font-semibold tracking-wide text-muted uppercase">
          <tr>
            {columns.map((c) => (
              <th key={c.header} scope="col" className={`px-4 py-3 whitespace-nowrap ${align(c)}`}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {rows.map((row) => (
            <tr key={rowKey(row)} className="hover:bg-surface/60">
              {columns.map((c) => (
                <td key={c.header} className={`px-4 py-2.5 align-middle whitespace-nowrap ${align(c)}`}>
                  {c.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
        <tfoot className="border-t-2 border-line bg-surface font-semibold text-ink">
          <tr>
            {columns.map((c, i) => (
              <td key={c.header} className={`px-4 py-2.5 whitespace-nowrap ${align(c)}`}>
                {i === 0 ? 'Total' : (c.total?.(totals) ?? '')}
              </td>
            ))}
          </tr>
        </tfoot>
      </table>
    </div>
  )
}

/** "District: Huye · Season: 2026B" from the scope the backend actually used. */
function scopeText(scope, names) {
  const parts = [
    `District: ${scope.districtId != null ? (names.district(scope.districtId) ?? `#${scope.districtId}`) : 'all districts'}`,
  ]
  if (names.withSeason) {
    parts.push(
      `Season: ${scope.seasonId != null ? (names.season(scope.seasonId) ?? `#${scope.seasonId}`) : 'all seasons'}`,
    )
  }
  return parts.join(' · ')
}

/**
 * Loading, error and empty states, then the report: what it covers, headline numbers, chart, table and how
 * it is calculated (the backend's basis text, the single definition every district uses).
 */
export function ReportView({ query, names, emptyMessage, tiles, chart, table }) {
  if (query.isPending) {
    return (
      <div className="py-10 text-center text-brand-700">
        <Spinner label="Preparing the report" />
      </div>
    )
  }
  if (query.error) return <Alert tone="error">{query.error.message}</Alert>

  const report = query.data
  return (
    <section aria-label={report.report} className="space-y-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-lg font-semibold text-ink">{report.report}</h2>
        <p className="text-xs text-muted">Generated {formatDateTime(report.generatedAt)}</p>
      </div>
      <p className="text-sm text-muted">{scopeText(report.scope, names)}</p>
      {report.scope.fieldOfficerId != null && (
        <Alert tone="info">Only the offerings assigned to you are counted.</Alert>
      )}
      {report.rows.length === 0 ? (
        <p className="rounded-lg border border-line bg-white px-4 py-8 text-center text-muted">{emptyMessage}</p>
      ) : (
        <>
          <StatTiles tiles={tiles(report.totals)} />
          {chart(report)}
          {table(report)}
        </>
      )}
      <details className="rounded-lg border border-line bg-white px-4 py-3 text-sm">
        <summary className="cursor-pointer font-medium text-ink">How this report is calculated</summary>
        <p className="mt-2 text-muted">{report.basis}</p>
      </details>
    </section>
  )
}
