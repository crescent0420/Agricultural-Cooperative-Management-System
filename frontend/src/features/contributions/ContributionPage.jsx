import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { adjustContribution, getContribution, listAdjustments, listContributionPayments } from '../../api/contributions'
import { toApiError } from '../../api/errors'
import { confirmPayment, failPayment, recordPayment } from '../../api/payments'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Spinner } from '../../components/ui/Spinner'
import { usePagedList } from '../../hooks/usePagedList'
import { formatAmount, formatDateTime } from '../../utils/format'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { ReasonDialog } from '../results/ReasonDialog'
import { AdjustAmountDialog } from './AdjustAmountDialog'
import { ConfirmPaymentDialog } from './ConfirmPaymentDialog'
import { ContributionFlags } from './ContributionFlags'
import { METHOD_LABELS, PAYMENT_SORTABLE } from './contributionFields'
import { PaymentStatusBadge } from './PaymentStatusBadge'
import { RecordPaymentDialog } from './RecordPaymentDialog'

const ADJUSTMENTS = { page: 0, size: 100, sortBy: 'adjustedAt', direction: 'desc' }

/**
 * /contributions/:id: the balance panel, the payments and (staff only, P2) the adjustment history
 * (URD FR-FIN-03..06). The Finance Officer records, confirms and fails payments and adjusts the amount.
 * A farmer sees only their own contribution (403 otherwise), read-only.
 */
export function ContributionPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const canManage = hasPermission('PAYMENT_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'requestedAt', direction: 'desc' }, PAYMENT_SORTABLE)
  const [dialog, setDialog] = useState(null) // { kind: 'record' | 'adjust' | 'confirm' | 'fail', payment? }

  const contribution = useQuery({ queryKey: ['contributions', 'detail', id], queryFn: () => getContribution(id) })
  const payments = useQuery({
    queryKey: ['contributions', 'payments', id, list.params],
    queryFn: () => listContributionPayments(id, list.params),
    placeholderData: keepPreviousData,
  })
  const adjustments = useQuery({
    queryKey: ['contributions', 'adjustments', id],
    queryFn: () => listAdjustments(id, ADJUSTMENTS),
    enabled: hasPermission('CONTRIBUTION_LIST'),
  })
  // Every change moves the balance: reload the contribution, its payments and its history.
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['contributions'] })

  const record = useMutation({
    mutationFn: (payment) => recordPayment(payment),
    onSuccess: async ({ created, payment }) => {
      await refresh()
      if (created) toast.success(`Payment of ${formatAmount(payment.amount)} recorded; it counts once confirmed`)
      else toast.success('Already received: the original payment was kept, nothing was added')
    },
  })
  const adjust = useMutation({
    mutationFn: (values) => adjustContribution(id, values),
    onSuccess: async (c) => {
      await refresh()
      toast.success(`Amount due changed to ${formatAmount(c.amountDue)}`)
    },
  })

  if (contribution.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the contribution" className="size-8" />
      </div>
    )
  }
  if (contribution.error) {
    const error = toApiError(contribution.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const c = contribution.data
  const back = hasPermission('CONTRIBUTION_LIST')
    ? { to: '/contributions', label: '← Contributions' }
    : { to: '/my/contributions', label: '← My contributions' }
  const balance = [
    { label: 'Amount due', value: formatAmount(c.amountDue) },
    { label: 'Paid (confirmed)', value: formatAmount(c.confirmedPaid) },
    { label: 'Pending', value: formatAmount(c.pendingAmount) },
    { label: 'Outstanding', value: <span className="text-lg font-semibold">{formatAmount(c.outstanding)}</span> },
    { label: 'Paid', value: <span className="text-lg font-semibold">{c.paidPercent}%</span> },
  ]

  const paymentColumns = [
    { header: 'Requested', sortField: 'requestedAt', cell: (p) => formatDateTime(p.requestedAt) },
    {
      header: 'Transaction reference',
      cell: (p) => <span className="font-mono text-xs">{p.transactionReference}</span>,
    },
    { header: 'Amount', sortField: 'amount', className: 'text-right', cell: (p) => formatAmount(p.amount) },
    { header: 'Method', cell: (p) => METHOD_LABELS[p.method] ?? p.method },
    { header: 'Status', sortField: 'status', cell: (p) => <PaymentStatusBadge status={p.status} /> },
    {
      header: 'Details',
      cell: (p) =>
        p.status === 'CONFIRMED' ? (
          <span className="text-xs">
            Provider ref. <span className="font-mono">{p.providerReference}</span>
            <span className="block text-muted">{formatDateTime(p.confirmedAt)}</span>
          </span>
        ) : p.status === 'FAILED' ? (
          <span className="text-xs">
            {p.failureReason}
            <span className="block text-muted">{formatDateTime(p.failedAt)}</span>
          </span>
        ) : (
          <span className="text-xs text-muted">Waiting for confirmation</span>
        ),
    },
  ]
  if (canManage) {
    // URD FR-FIN-06: the buttons appear on PENDING payments only.
    paymentColumns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (p) =>
        p.status === 'PENDING' && (
          <span className="inline-flex gap-3">
            <Button
              variant="link"
              onClick={() => setDialog({ kind: 'confirm', payment: p })}
              aria-label={`Confirm ${p.transactionReference}`}
            >
              Confirm
            </Button>
            <Button
              variant="link"
              onClick={() => setDialog({ kind: 'fail', payment: p })}
              aria-label={`Mark ${p.transactionReference} failed`}
            >
              Mark failed
            </Button>
          </span>
        ),
    })
  }

  return (
    <>
      <Link to={back.to} className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        {back.label}
      </Link>
      <PageHeader
        title={`${c.farmerName}, ${c.seasonName}`}
        description={`${c.membershipNumber} · input contribution`}
        actions={
          canManage && (
            <>
              <Button variant="secondary" onClick={() => setDialog({ kind: 'adjust' })}>
                Adjust amount
              </Button>
              <Button onClick={() => setDialog({ kind: 'record' })}>Record payment</Button>
            </>
          )
        }
      />
      <section aria-label="Balance" className="mb-6 rounded-lg border border-line bg-white p-6">
        <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-3 lg:grid-cols-5">
          {balance.map(({ label, value }) => (
            <div key={label}>
              <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</dt>
              <dd className="mt-1 text-ink">{value}</dd>
            </div>
          ))}
        </dl>
        <div className="mt-4 border-t border-line pt-4">
          <ContributionFlags contribution={c} />
        </div>
      </section>

      <section className="mb-6 space-y-3">
        <h2 className="text-lg font-semibold">Payments</h2>
        <DataTable
          caption="Payments"
          columns={paymentColumns}
          rows={payments.data?.content}
          rowKey={(p) => p.id}
          loading={payments.isPending}
          error={payments.error}
          emptyMessage="No payments recorded yet."
          sort={list.params}
          onSort={list.toggleSort}
        />
        <Pagination page={payments.data} onPage={list.setPage} onSize={list.setSize} />
      </section>

      {hasPermission('CONTRIBUTION_LIST') && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold">Adjustment history</h2>
          <DataTable
            caption="Adjustment history"
            columns={[
              { header: 'When', cell: (a) => formatDateTime(a.adjustedAt) },
              { header: 'Amount', cell: (a) => `${formatAmount(a.oldAmount)} → ${formatAmount(a.newAmount)}` },
              { header: 'Reason', cell: (a) => a.reason },
              { header: 'By', cell: (a) => a.adjustedByUsername },
            ]}
            rows={adjustments.data?.content}
            rowKey={(a) => a.id}
            loading={adjustments.isPending}
            error={adjustments.error}
            emptyMessage="The amount has never been adjusted."
          />
        </section>
      )}

      {dialog?.kind === 'record' && (
        <RecordPaymentDialog contribution={c} onRecord={record.mutateAsync} onClose={() => setDialog(null)} />
      )}
      {dialog?.kind === 'adjust' && (
        <AdjustAmountDialog contribution={c} onAdjust={adjust.mutateAsync} onClose={() => setDialog(null)} />
      )}
      {dialog?.kind === 'confirm' && (
        <ConfirmPaymentDialog
          payment={dialog.payment}
          onConfirm={async (providerReference) => {
            await confirmPayment(dialog.payment.id, providerReference)
            await refresh()
            toast.success('Payment confirmed; the balance is updated')
          }}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog?.kind === 'fail' && (
        <ReasonDialog
          title="Mark this payment as failed?"
          intro={`${formatAmount(dialog.payment.amount)}, reference ${dialog.payment.transactionReference}. A failed payment never counts and cannot be confirmed later.`}
          confirmLabel="Mark failed"
          onConfirm={async (reason) => {
            await failPayment(dialog.payment.id, reason)
            await refresh()
            toast.success('Payment marked as failed')
          }}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}
