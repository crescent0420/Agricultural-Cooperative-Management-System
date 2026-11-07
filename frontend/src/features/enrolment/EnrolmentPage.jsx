import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { getEnrolment } from '../../api/enrolments'
import { toApiError } from '../../api/errors'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Spinner } from '../../components/ui/Spinner'
import { formatDateTime } from '../../utils/format'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { GetPermitDialog } from '../permits/GetPermitDialog'
import { CancelEnrolmentDialog } from './CancelEnrolmentDialog'
import { EnrolmentStatusBadge } from './EnrolmentStatusBadge'
import { useCancelEnrolment } from './useCancelEnrolment'

/**
 * /enrolments/:id: one enrolment (URD FR-ENR-06). A farmer may open only their own; staff only those of
 * offerings they may see. The backend answers 403 otherwise.
 */
export function EnrolmentPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const queryClient = useQueryClient()
  const { cancel, reasonRequired } = useCancelEnrolment()
  const [cancelling, setCancelling] = useState(false)
  const [gettingPermit, setGettingPermit] = useState(false)
  const enrolment = useQuery({ queryKey: ['enrolments', 'detail', id], queryFn: () => getEnrolment(id) })

  if (enrolment.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the enrolment" className="size-8" />
      </div>
    )
  }
  if (enrolment.error) {
    const error = toApiError(enrolment.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const e = enrolment.data
  const isStaff = hasPermission('ENROLMENT_LIST_FOR_OFFERING')
  const back = isStaff
    ? { to: `/offerings/${e.offeringId}/enrolments`, label: '← Offering enrolments' }
    : { to: '/my/enrolments', label: '← My enrolments' }
  const rows = [
    {
      label: 'Farmer',
      value:
        hasPermission('FARMER_READ') && isStaff ? (
          <Link to={`/farmers/${e.farmerId}`} className="font-medium text-brand-700 hover:underline">
            {e.farmerName}
          </Link>
        ) : (
          e.farmerName
        ),
    },
    { label: 'Membership number', value: <span className="font-mono">{e.membershipNumber}</span> },
    {
      label: 'Crop programme',
      value: isStaff ? (
        <Link to={`/offerings/${e.offeringId}`} className="font-medium text-brand-700 hover:underline">
          {e.programmeName}
        </Link>
      ) : (
        e.programmeName
      ),
    },
    { label: 'Season', value: e.seasonName },
    { label: 'Hectares', value: `${e.hectares} ha` },
    { label: 'Status', value: <EnrolmentStatusBadge status={e.status} /> },
    { label: 'Enrolled', value: formatDateTime(e.enrolledAt) },
  ]
  if (e.status === 'CANCELLED') {
    rows.push({ label: 'Cancelled', value: formatDateTime(e.cancelledAt) })
    rows.push({ label: 'Reason', value: e.cancellationReason ?? '—' })
  }

  return (
    <>
      <Link to={back.to} className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        {back.label}
      </Link>
      <PageHeader
        title={`${e.programmeName}, ${e.seasonName}`}
        description={`Enrolment of ${e.farmerName}`}
        actions={
          e.status === 'ENROLLED' && (
            <>
              {hasPermission('PERMIT_GENERATE') && <Button onClick={() => setGettingPermit(true)}>Get permit</Button>}
              {hasPermission('ENROLMENT_CANCEL') && (
                <Button variant="secondary" onClick={() => setCancelling(true)}>
                  Cancel enrolment
                </Button>
              )}
            </>
          )
        }
      />
      <section className="rounded-lg border border-line bg-white p-6">
        <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
          {rows.map(({ label, value }) => (
            <div key={label}>
              <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</dt>
              <dd className="mt-1 text-ink">{value}</dd>
            </div>
          ))}
        </dl>
      </section>
      {gettingPermit && <GetPermitDialog enrolment={e} onClose={() => setGettingPermit(false)} />}
      {cancelling && (
        <CancelEnrolmentDialog
          enrolment={e}
          reasonRequired={reasonRequired}
          onCancel={async (reason) => {
            const saved = await cancel({ id: e.id, reason })
            queryClient.setQueryData(['enrolments', 'detail', id], saved)
          }}
          onClose={() => setCancelling(false)}
        />
      )}
    </>
  )
}
