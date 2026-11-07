import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { listOfferingEnrolments } from '../../api/enrolments'
import { toApiError } from '../../api/errors'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../utils/format'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { useOffering } from '../offerings/useOffering'
import { CancelEnrolmentDialog } from './CancelEnrolmentDialog'
import { EnrolmentStatusBadge } from './EnrolmentStatusBadge'
import { ENROLMENT_SORTABLE } from './enrolmentFields'
import { useCancelEnrolment } from './useCancelEnrolment'

/**
 * Offering page, Enrolments tab (URD FR-ENR-05). A Field Officer sees only the offerings assigned to them:
 * for another officer's offering the backend answers 403 and the tab shows "Not available to you" (E3).
 * The backend lists one status at a time (ENROLLED by default).
 */
export function OfferingEnrolmentsTab() {
  const offering = useOffering()
  const { user, hasPermission } = useAuth()
  const list = usePagedList({ sortBy: 'enrolledAt', direction: 'asc' }, ENROLMENT_SORTABLE)
  const status = list.filter('status') || 'ENROLLED'
  const { cancel, reasonRequired } = useCancelEnrolment()
  const [cancelling, setCancelling] = useState(null)

  const enrolments = useQuery({
    queryKey: ['enrolments', 'offering', offering.id, list.params, status],
    queryFn: () => listOfferingEnrolments(offering.id, list.params, status),
    placeholderData: keepPreviousData,
  })
  if (enrolments.error && toApiError(enrolments.error).status === 403) return <ForbiddenPage />

  const columns = [
    {
      header: 'Membership no.',
      cell: (e) => (
        <Link to={`/enrolments/${e.id}`} className="font-mono text-xs text-brand-700 hover:underline">
          {e.membershipNumber}
        </Link>
      ),
    },
    {
      header: 'Farmer',
      cell: (e) =>
        hasPermission('FARMER_READ') ? (
          <Link to={`/farmers/${e.farmerId}`} className="font-medium text-brand-700 hover:underline">
            {e.farmerName}
          </Link>
        ) : (
          <span className="font-medium">{e.farmerName}</span>
        ),
    },
    { header: 'Hectares', className: 'text-right', cell: (e) => e.hectares },
    { header: 'Enrolled', sortField: 'enrolledAt', cell: (e) => formatDateTime(e.enrolledAt) },
    { header: 'Status', cell: (e) => <EnrolmentStatusBadge status={e.status} /> },
  ]
  if (status === 'CANCELLED') {
    columns.push({ header: 'Reason', cell: (e) => e.cancellationReason ?? '—' })
  }
  // FR-RES-01: the offering's own Field Officer enters each farmer's scores.
  if (status === 'ENROLLED' && hasPermission('RESULT_ENTER') && user?.id === offering.fieldOfficerId) {
    columns.push({
      header: 'Scores',
      cell: (e) => (
        <Link
          to={`/enrolments/${e.id}/scores`}
          aria-label={`Scores of ${e.farmerName}`}
          className="font-semibold text-brand-700 hover:underline"
        >
          Scores
        </Link>
      ),
    })
  }
  if (status === 'ENROLLED' && hasPermission('ENROLMENT_CANCEL')) {
    columns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (e) => (
        <Button variant="link" onClick={() => setCancelling(e)} aria-label={`Cancel the enrolment of ${e.farmerName}`}>
          Cancel
        </Button>
      ),
    })
  }

  return (
    <section className="space-y-3">
      <div className="max-w-xs">
        <Select label="Status" value={status} onChange={(e) => list.setFilter('status', e.target.value)}>
          <option value="ENROLLED">Enrolled</option>
          <option value="CANCELLED">Cancelled</option>
        </Select>
      </div>
      <DataTable
        caption="Enrolled farmers"
        columns={columns}
        rows={enrolments.data?.content}
        rowKey={(e) => e.id}
        loading={enrolments.isPending}
        error={enrolments.error}
        emptyMessage={status === 'ENROLLED' ? 'No farmer has enrolled yet.' : 'No cancelled enrolments.'}
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
    </section>
  )
}
