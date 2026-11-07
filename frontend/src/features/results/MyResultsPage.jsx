import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listMyResults } from '../../api/results'
import { PageHeader } from '../../components/layout/PageHeader'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../utils/format'
import { RESULT_SORTABLE, scoreText } from './resultFields'

/** /my/results: the farmer's own published results (URD FR-RES-07). The backend never returns the others. */
export function MyResultsPage() {
  const list = usePagedList({ sortBy: 'id', direction: 'desc' }, RESULT_SORTABLE)
  const results = useQuery({
    queryKey: ['results', 'mine', list.params],
    queryFn: () => listMyResults(list.params),
    placeholderData: keepPreviousData,
  })
  return (
    <>
      <PageHeader title="My results" description="Your published season results, with their quality grade." />
      <DataTable
        caption="My results"
        columns={[
          {
            header: 'Programme',
            cell: (r) => (
              <Link to={`/results/${r.id}`} className="font-medium text-brand-700 hover:underline">
                {r.programmeName}
              </Link>
            ),
          },
          { header: 'Season', cell: (r) => r.seasonName },
          {
            header: 'Final score',
            sortField: 'finalScore',
            className: 'text-right',
            cell: (r) => scoreText(r.finalScore),
          },
          { header: 'Grade', cell: (r) => <span className="font-semibold">{r.grade ?? '—'}</span> },
          { header: 'Published', cell: (r) => formatDateTime(r.publishedAt) },
        ]}
        rows={results.data?.content}
        rowKey={(r) => r.id}
        loading={results.isPending}
        error={results.error}
        emptyMessage="No published results yet. They appear here once the District Agronomist approves them."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={results.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
