import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toApiError } from '../../api/errors'
import { approveResult, correctResult, getResult, listCorrections, returnResult } from '../../api/results'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { DataTable } from '../../components/ui/DataTable'
import { Spinner } from '../../components/ui/Spinner'
import { formatDateTime } from '../../utils/format'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { CorrectionDialog } from './CorrectionDialog'
import { ReasonDialog } from './ReasonDialog'
import { scoreText } from './resultFields'
import { ResultStatusBadge } from './ResultStatusBadge'

/**
 * /results/:id: one result with its scores, final score, grade, stage dates and correction history
 * (URD FR-RES-04, 05, 06). Buttons follow role and stage (R4); the backend checks the district and the
 * assignment on every action. A farmer reaches only their own published results (403 otherwise).
 */
export function ResultPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [dialog, setDialog] = useState(null) // 'approve' | 'return' | 'correct'
  const [approveError, setApproveError] = useState(null)

  const result = useQuery({ queryKey: ['results', 'detail', id], queryFn: () => getResult(id) })
  const corrections = useQuery({ queryKey: ['results', 'corrections', id], queryFn: () => listCorrections(id) })
  const settle = async (saved, message) => {
    queryClient.setQueryData(['results', 'detail', id], saved)
    await queryClient.invalidateQueries({ queryKey: ['results'] })
    toast.success(message)
  }
  const approve = useMutation({
    mutationFn: () => approveResult(id),
    onSuccess: async (saved) => {
      setDialog(null)
      await settle(saved, `Result published: ${saved.finalScore} (${saved.grade})`)
    },
    onError: (error) => setApproveError(toApiError(error)),
  })

  if (result.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the result" className="size-8" />
      </div>
    )
  }
  if (result.error) {
    const error = toApiError(result.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const r = result.data
  const isStaff = hasPermission('INSPECTION_READ')
  const back = isStaff
    ? { to: `/offerings/${r.offeringId}/results`, label: '← Offering results' }
    : { to: '/my/results', label: '← My results' }
  const actions = []
  if (hasPermission('RESULT_APPROVE') && r.status === 'SUBMITTED') {
    actions.push(
      <Button key="return" variant="secondary" onClick={() => setDialog('return')}>
        Return to draft
      </Button>,
      <Button
        key="approve"
        onClick={() => {
          setApproveError(null)
          setDialog('approve')
        }}
      >
        Approve and publish
      </Button>,
    )
  }
  if (hasPermission('RESULT_CORRECT') && r.status === 'PUBLISHED') {
    actions.push(
      <Button key="correct" variant="secondary" onClick={() => setDialog('correct')}>
        Correct a score
      </Button>,
    )
  }
  if (hasPermission('RESULT_ENTER') && r.status === 'DRAFT') {
    actions.push(
      <Link
        key="scores"
        to={`/enrolments/${r.enrolmentId}/scores`}
        className="inline-flex items-center rounded-lg border border-line bg-white px-4 py-2.5 text-sm font-semibold text-ink hover:bg-surface"
      >
        Edit scores
      </Link>,
    )
  }

  const facts = [
    { label: 'Status', value: <ResultStatusBadge status={r.status} /> },
    { label: 'Final score', value: <span className="text-lg font-semibold">{scoreText(r.finalScore)}</span> },
    { label: 'Grade', value: <span className="text-lg font-semibold">{r.grade ?? '—'}</span> },
    { label: 'Submitted', value: formatDateTime(r.submittedAt) },
    { label: 'Published', value: formatDateTime(r.publishedAt) },
    { label: 'Corrections', value: r.correctionCount },
  ]

  return (
    <>
      <Link to={back.to} className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        {back.label}
      </Link>
      <PageHeader
        title={`${r.programmeName}, ${r.seasonName}`}
        description={`${r.farmerName} · ${r.membershipNumber}`}
        actions={actions.length > 0 && actions}
      />
      <section className="mb-6 rounded-lg border border-line bg-white p-6">
        <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-3 lg:grid-cols-6">
          {facts.map(({ label, value }) => (
            <div key={label}>
              <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</dt>
              <dd className="mt-1 text-ink">{value}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="mb-6 space-y-3">
        <h2 className="text-lg font-semibold">Scores</h2>
        <DataTable
          caption="Scores"
          columns={[
            { header: 'Inspection', cell: (s) => <span className="font-medium">{s.inspectionName}</span> },
            { header: 'Weight', className: 'text-right', cell: (s) => `${s.weight}%` },
            {
              header: 'Score',
              className: 'text-right',
              cell: (s) => (
                <span>
                  {scoreText(s.currentScore)} <span className="text-muted">/ {s.maxScore}</span>
                  {s.corrected && (
                    <span className="block text-xs text-muted">
                      corrected; originally <s>{scoreText(s.originalScore)}</s>
                    </span>
                  )}
                </span>
              ),
            },
          ]}
          rows={r.scores}
          rowKey={(s) => s.inspectionId}
          emptyMessage="No inspections."
        />
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Correction history</h2>
        <DataTable
          caption="Correction history"
          columns={[
            { header: 'When', cell: (c) => formatDateTime(c.correctedAt) },
            { header: 'Inspection', cell: (c) => c.inspectionName },
            { header: 'Score', cell: (c) => `${c.originalScore} → ${c.correctedScore}` },
            { header: 'Final score', cell: (c) => `${c.originalFinalScore} → ${c.correctedFinalScore}` },
            { header: 'Grade', cell: (c) => `${c.originalGrade} → ${c.correctedGrade}` },
            { header: 'Reason', cell: (c) => c.reason },
            { header: 'By', cell: (c) => c.correctedByUsername },
          ]}
          rows={corrections.data}
          rowKey={(c) => c.id}
          loading={corrections.isPending}
          error={corrections.error}
          emptyMessage="No corrections."
        />
      </section>

      <ConfirmDialog
        open={dialog === 'approve'}
        title={`Publish the result of ${r.farmerName}?`}
        confirmLabel="Approve and publish"
        busy={approve.isPending}
        onConfirm={() => approve.mutate()}
        onCancel={() => setDialog(null)}
      >
        <div className="space-y-3">
          {approveError && <Alert tone="error">{approveError.message}</Alert>}
          <p>
            The farmer will see the final score and grade. After publication, a score can change only through a
            correction with a reason.
          </p>
        </div>
      </ConfirmDialog>
      {dialog === 'return' && (
        <ReasonDialog
          title="Return this result to the Field Officer?"
          intro="It goes back to DRAFT so the Field Officer can change the scores and submit again."
          confirmLabel="Return to draft"
          onConfirm={async (reason) => settle(await returnResult(id, reason), 'Result returned to draft')}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'correct' && (
        <CorrectionDialog
          result={r}
          onCorrect={async (correction) => {
            const saved = await correctResult(id, correction)
            await settle(saved, `Score corrected; final score now ${saved.finalScore} (${saved.grade})`)
          }}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}
