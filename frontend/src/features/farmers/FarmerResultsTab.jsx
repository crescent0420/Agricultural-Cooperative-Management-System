import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listFarmerResults } from '../../api/farmers'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDate } from '../../utils/format'
import { useFarmer } from './useFarmer'

/** The fields GET /api/farmers/{id}/results can sort by (ResultService.SORTABLE_FIELDS). */
const SORTABLE = ['id', 'createdAt', 'finalScore', 'status']

const RESULT_STATUS = { DRAFT: 'Draft', SUBMITTED: 'Submitted', PUBLISHED: 'Published' }

/**
 * Results tab (URD FR-FAR-09), read-only. The backend decides which results appear: a Field Officer sees
 * those of their own offerings, an Agronomist those of their district, the Registrar all of them.
 */
export function FarmerResultsTab() {
  const farmer = useFarmer()
  const list = usePagedList({ sortBy: 'id', direction: 'desc' }, SORTABLE)
  const results = useQuery({
    queryKey: ['farmers', 'results', farmer.id, list.params],
    queryFn: () => listFarmerResults(farmer.id, list.params),
    placeholderData: keepPreviousData,
  })

  return (
    <section className="space-y-4">
      <h2 className="text-lg font-semibold">Season results</h2>
      <DataTable
        caption="Season results"
        columns={[
          { header: 'Programme', cell: (r) => <span className="font-medium">{r.programmeName}</span> },
          { header: 'Season', cell: (r) => r.seasonName },
          { header: 'Status', sortField: 'status', cell: (r) => RESULT_STATUS[r.status] ?? r.status },
          {
            header: 'Final score',
            sortField: 'finalScore',
            className: 'text-right',
            cell: (r) => (r.finalScore == null ? '—' : r.finalScore),
          },
          { header: 'Grade', cell: (r) => r.grade ?? '—' },
          { header: 'Published', cell: (r) => formatDate(r.publishedAt) },
        ]}
        rows={results.data?.content}
        rowKey={(r) => r.id}
        loading={results.isPending}
        error={results.error}
        emptyMessage="No results you can see for this farmer yet."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={results.data} onPage={list.setPage} onSize={list.setSize} />
    </section>
  )
}
