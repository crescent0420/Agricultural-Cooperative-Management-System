import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { listPermitAttempts } from '../../api/permits'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../utils/format'
import { FarmerPicker } from '../contributions/FarmerPicker'

const OUTCOMES = {
  ISSUED: { label: 'Issued', tone: 'success' },
  ALREADY_ISSUED: { label: 'Already issued', tone: 'neutral' },
  REFUSED: { label: 'Refused', tone: 'danger' },
}
const SORTABLE = ['id', 'attemptedAt']

/** /permits/attempts: every attempt to obtain a permit, issued or refused, with the reason (URD FR-PER-04). */
export function PermitAttemptsPage() {
  const { hasPermission } = useAuth()
  const list = usePagedList({ sortBy: 'attemptedAt', direction: 'desc' }, SORTABLE)
  const farmerId = list.filter('farmerId')
  const enrolmentId = list.filter('enrolmentId')
  const outcome = list.filter('outcome')
  const [enrolmentText, setEnrolmentText] = useState(enrolmentId)
  const farmer = farmerId
    ? { id: Number(farmerId), fullName: list.filter('farmerName'), membershipNumber: list.filter('farmerNumber') }
    : null

  const attempts = useQuery({
    queryKey: ['permits', 'attempts', list.params, farmerId, enrolmentId, outcome],
    queryFn: () =>
      listPermitAttempts(list.params, {
        farmerId: farmerId ? Number(farmerId) : undefined,
        enrolmentId: enrolmentId ? Number(enrolmentId) : undefined,
        outcome,
      }),
    placeholderData: keepPreviousData,
  })
  // T1: the attempt carries the farmer's id only; the Registrar can open the farmer, the Administrator cannot (G6).
  const farmerCell = (a) =>
    hasPermission('FARMER_READ') ? (
      <Link to={`/farmers/${a.farmerId}`} className="font-medium text-brand-700 hover:underline">
        Farmer #{a.farmerId}
      </Link>
    ) : (
      `Farmer #${a.farmerId}`
    )

  return (
    <>
      <PageHeader title="Permit attempts" description="Every request for a collection permit, issued or refused." />
      <div className="mb-4 grid gap-3 rounded-lg border border-line bg-white p-4 sm:grid-cols-3">
        <FarmerPicker
          value={farmer}
          onChange={(f) =>
            list.setFilters({
              farmerId: f?.id ?? '',
              farmerName: f?.fullName ?? '',
              farmerNumber: f?.membershipNumber ?? '',
            })
          }
        />
        <form
          onSubmit={(e) => {
            e.preventDefault()
            list.setFilter('enrolmentId', enrolmentText.trim())
          }}
          className="flex items-end gap-2"
        >
          <TextField
            label="Enrolment number"
            className="flex-1"
            inputMode="numeric"
            value={enrolmentText}
            onChange={(e) => setEnrolmentText(e.target.value.replace(/\D/g, ''))}
          />
          <Button type="submit" variant="secondary">
            Filter
          </Button>
        </form>
        <Select label="Outcome" value={outcome} onChange={(e) => list.setFilter('outcome', e.target.value)}>
          <option value="">All outcomes</option>
          {Object.entries(OUTCOMES).map(([value, { label }]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </div>
      <DataTable
        caption="Permit attempts"
        columns={[
          { header: 'When', sortField: 'attemptedAt', cell: (a) => formatDateTime(a.attemptedAt) },
          { header: 'Farmer', cell: farmerCell },
          { header: 'Enrolment', cell: (a) => `#${a.enrolmentId}` },
          { header: 'By', cell: (a) => a.username },
          {
            header: 'Outcome',
            cell: (a) => <Badge tone={OUTCOMES[a.outcome]?.tone}>{OUTCOMES[a.outcome]?.label ?? a.outcome}</Badge>,
          },
          { header: 'Reason', cell: (a) => a.reason ?? '—' },
        ]}
        rows={attempts.data?.content}
        rowKey={(a) => a.id}
        loading={attempts.isPending}
        error={attempts.error}
        emptyMessage="No permit attempts match these filters."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={attempts.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
