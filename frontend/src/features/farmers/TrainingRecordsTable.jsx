import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listFarmerTrainings } from '../../api/farmers'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDate } from '../../utils/format'

/** The fields GET /api/farmers/{id}/trainings can sort by (FarmerTrainingService.SORTABLE_FIELDS). */
const SORTABLE = ['id', 'completedOn', 'recordedAt']

/** A farmer's completed trainings, newest first (URD FR-FAR-06). Used on the farmer page and on "My profile". */
export function TrainingRecordsTable({ farmerId }) {
  const list = usePagedList({ sortBy: 'completedOn', direction: 'desc' }, SORTABLE)
  const records = useQuery({
    queryKey: ['farmers', 'trainings', farmerId, list.params],
    queryFn: () => listFarmerTrainings(farmerId, list.params),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <DataTable
        caption="Completed trainings"
        columns={[
          { header: 'Training', cell: (r) => <span className="font-medium">{r.trainingName}</span> },
          { header: 'Completed on', sortField: 'completedOn', cell: (r) => formatDate(r.completedOn) },
        ]}
        rows={records.data?.content}
        rowKey={(r) => r.id}
        loading={records.isPending}
        error={records.error}
        emptyMessage="No completed trainings recorded yet."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={records.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
