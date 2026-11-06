import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { listMyEnrolments } from '../../api/enrolments'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../utils/format'
import { SeasonFilter } from '../offerings/SeasonFilter'
import { CancelEnrolmentDialog } from './CancelEnrolmentDialog'
import { EnrolmentStatusBadge } from './EnrolmentStatusBadge'
import { ENROLMENT_SORTABLE } from './enrolmentFields'
import { useCancelEnrolment } from './useCancelEnrolment'

/** /my/enrolments: the farmer's own enrolments, by season and status; cancel (URD FR-ENR-03, FR-ENR-04). */
export function MyEnrolmentsPage() {
  const { hasPermission } = useAuth()
  const list = usePagedList({ sortBy: 'enrolledAt', direction: 'desc' }, ENROLMENT_SORTABLE)
  const seasonId = list.filter('seasonId')
  const status = list.filter('status')
  const { cancel, reasonRequired } = useCancelEnrolment()
  const [cancelling, setCancelling] = useState(null)

  const enrolments = useQuery({
    queryKey: ['enrolments', 'mine', list.params, seasonId, status],
    queryFn: () => listMyEnrolments(list.params, { seasonId: seasonId ? Number(seasonId) : undefined, status }),
    placeholderData: keepPreviousData,
  })

  const columns = [
    {
      header: 'Programme',
      cell: (e) => (
        <Link to={`/enrolments/${e.id}`} className="font-medium text-brand-700 hover:underline">
          {e.programmeName}
        </Link>
      ),
    },
    { header: 'Season', cell: (e) => e.seasonName },
    { header: 'Hectares', className: 'text-right', cell: (e) => e.hectares },
    { header: 'Status', sortField: 'status', cell: (e) => <EnrolmentStatusBadge status={e.status} /> },
    { header: 'Enrolled', sortField: 'enrolledAt', cell: (e) => formatDateTime(e.enrolledAt) },
    {
      header: 'Cancelled',
      cell: (e) =>
        e.status === 'CANCELLED' ? (
          <span>
            {formatDateTime(e.cancelledAt)}
            {e.cancellationReason && <span className="block text-xs text-muted">Reason: {e.cancellationReason}</span>}
          </span>
        ) : (
          '—'
        ),
    },
  ]
  if (hasPermission('ENROLMENT_CANCEL')) {
    columns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (e) =>
        e.status === 'ENROLLED' && (
          <Button variant="link" onClick={() => setCancelling(e)} aria-label={`Cancel ${e.programmeName}`}>
            Cancel
          </Button>
        ),
    })
  }

  return (
    <>
      <PageHeader title="My enrolments" description="The crop programmes you are enrolled in, season by season." />
      <div className="mb-4 grid max-w-xl gap-3 sm:grid-cols-2">
        <SeasonFilter value={seasonId} onChange={(v) => list.setFilter('seasonId', v)} />
        <Select label="Status" value={status} onChange={(e) => list.setFilter('status', e.target.value)}>
          <option value="">All</option>
          <option value="ENROLLED">Enrolled</option>
          <option value="CANCELLED">Cancelled</option>
        </Select>
      </div>
      <DataTable
        caption="My enrolments"
        columns={columns}
        rows={enrolments.data?.content}
        rowKey={(e) => e.id}
        loading={enrolments.isPending}
        error={enrolments.error}
        emptyMessage={
          seasonId || status ? 'No enrolments match these filters.' : 'You have not enrolled in any programme yet.'
        }
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={enrolments.data} onPage={list.setPage} onSize={list.setSize} />
      {cancelling && (
        <CancelEnrolmentDialog
          enrolment={cancelling}
          reasonRequired={reasonRequired}
          onCancel={(reason) => cancel({ id: cancelling.id, reason })}
          onClose={() => setCancelling(null)}
        />
      )}
    </>
  )
}
