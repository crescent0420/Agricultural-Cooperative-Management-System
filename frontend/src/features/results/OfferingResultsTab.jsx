import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { toApiError } from '../../api/errors'
import { listOfferingResults } from '../../api/results'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../utils/format'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { useOffering } from '../offerings/useOffering'
import { RESULT_SORTABLE, RESULT_STATUS_LABELS, scoreText } from './resultFields'
import { ResultStatusBadge } from './ResultStatusBadge'

/**
 * Offering page, Results tab (URD FR-RES-03). An Agronomist finds the results waiting for approval with the
 * Submitted filter (decision R3). The backend allows Registrar, Admin, the district's Agronomist and the
 * offering's Field Officer.
 */
export function OfferingResultsTab() {
  const offering = useOffering()
  const list = usePagedList({ sortBy: 'id', direction: 'asc' }, RESULT_SORTABLE)
  const status = list.filter('status')
  const results = useQuery({
    queryKey: ['results', 'offering', offering.id, list.params, status],
    queryFn: () => listOfferingResults(offering.id, list.params, status),
    placeholderData: keepPreviousData,
  })
  if (results.error && toApiError(results.error).status === 403) return <ForbiddenPage />

  return (
    <section className="space-y-3">
      <div className="max-w-xs">
        <Select label="Status" value={status} onChange={(e) => list.setFilter('status', e.target.value)}>
          <option value="">All</option>
          {Object.entries(RESULT_STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </div>
      <DataTable
        caption="Results"
        columns={[
          {
            header: 'Farmer',
            cell: (r) => (
              <Link to={`/results/${r.id}`} className="font-medium text-brand-700 hover:underline">
                {r.farmerName}
              </Link>
            ),
          },
          { header: 'Membership no.', cell: (r) => <span className="font-mono text-xs">{r.membershipNumber}</span> },
          { header: 'Status', sortField: 'status', cell: (r) => <ResultStatusBadge status={r.status} /> },
          {
            header: 'Final score',
            sortField: 'finalScore',
            className: 'text-right',
            cell: (r) => scoreText(r.finalScore),
          },
          { header: 'Grade', cell: (r) => r.grade ?? '—' },
          { header: 'Published', cell: (r) => formatDateTime(r.publishedAt) },
        ]}
        rows={results.data?.content}
        rowKey={(r) => r.id}
        loading={results.isPending}
        error={results.error}
        emptyMessage={status ? 'No results with this status.' : 'No scores have been entered yet.'}
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={results.data} onPage={list.setPage} onSize={list.setSize} />
    </section>
  )
}
