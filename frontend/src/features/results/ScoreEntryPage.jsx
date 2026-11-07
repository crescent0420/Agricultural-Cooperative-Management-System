import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router'
import { getEnrolment } from '../../api/enrolments'
import { toApiError } from '../../api/errors'
import { listFarmerResults } from '../../api/farmers'
import { listInspections } from '../../api/inspections'
import { enterScores, getResult, submitResult } from '../../api/results'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Spinner } from '../../components/ui/Spinner'
import { TextField } from '../../components/ui/TextField'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { scoreRules } from './resultFields'
import { ResultStatusBadge } from './ResultStatusBadge'

const A_FARMERS_RESULTS = { page: 0, size: 100, sortBy: 'id', direction: 'desc' }

/**
 * Decision R1: there is no "result of this enrolment" endpoint, so the farmer's own results (a short list) are
 * read and the one for this enrolment picked out. No result yet = the scores start empty.
 */
function useEnrolmentResult(enrolment) {
  const summaries = useQuery({
    queryKey: ['results', 'farmer', enrolment?.farmerId, 'for-scores'],
    queryFn: () => listFarmerResults(enrolment.farmerId, A_FARMERS_RESULTS),
    enabled: enrolment != null,
  })
  const resultId = summaries.data?.content.find((r) => r.enrolmentId === enrolment?.id)?.id
  const result = useQuery({
    queryKey: ['results', 'detail', resultId],
    queryFn: () => getResult(resultId),
    enabled: resultId != null,
  })
  return { pending: summaries.isPending || (resultId != null && result.isPending), result: result.data ?? null }
}

/**
 * /enrolments/:id/scores: the assigned Field Officer enters one score per inspection, saves the DRAFT and
 * submits it for approval (URD FR-RES-01, FR-RES-02). Scores change only while the result is a DRAFT.
 */
export function ScoreEntryPage() {
  const enrolmentId = Number(useParams().id)
  const enrolment = useQuery({
    queryKey: ['enrolments', 'detail', enrolmentId],
    queryFn: () => getEnrolment(enrolmentId),
  })
  const inspections = useQuery({
    queryKey: ['inspections', enrolment.data?.offeringId],
    queryFn: () => listInspections(enrolment.data.offeringId),
    enabled: enrolment.data != null,
  })
  const { pending, result } = useEnrolmentResult(enrolment.data)

  const error = enrolment.error ?? inspections.error
  if (error) {
    const apiError = toApiError(error)
    return apiError.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{apiError.message}</Alert>
  }
  if (enrolment.isPending || inspections.isPending || pending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the scores" className="size-8" />
      </div>
    )
  }
  // key: a fresh form when the stage changes (e.g. after submitting). Not when the first save creates the
  // draft: the fields already hold what was typed, and an open dialog must survive.
  return (
    <ScoreForm
      key={result?.status ?? 'DRAFT'}
      enrolment={enrolment.data}
      inspectionList={inspections.data}
      result={result}
    />
  )
}

function ScoreForm({ enrolment, inspectionList, result }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [failure, setFailure] = useState(null)
  const [confirming, setConfirming] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [blockedTried, setBlockedTried] = useState(false)
  const [busy, setBusy] = useState(false)
  const current = new Map((result?.scores ?? []).map((s) => [s.inspectionId, s.currentScore]))
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting, isDirty },
  } = useForm({
    defaultValues: Object.fromEntries(inspectionList.inspections.map((i) => [`s${i.id}`, current.get(i.id) ?? ''])),
  })

  const status = result?.status ?? 'DRAFT'
  const notEditable =
    enrolment.status !== 'ENROLLED'
      ? 'This enrolment was cancelled; no scores can be entered.'
      : status !== 'DRAFT'
        ? `This result is ${status === 'SUBMITTED' ? 'submitted and waiting for the District Agronomist' : 'published'}; scores can be changed only while it is a draft.`
        : undefined

  const entriesFrom = (values) =>
    inspectionList.inspections
      .filter((i) => values[`s${i.id}`] !== '' && values[`s${i.id}`] != null)
      .map((i) => ({ inspectionId: i.id, score: Number(values[`s${i.id}`]) }))

  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['results'] }),
      queryClient.invalidateQueries({ queryKey: ['inspections'] }),
    ])

  /** Saves the DRAFT; returns the saved result, or null when nothing was saved. */
  const save = async (values) => {
    const entries = entriesFrom(values)
    if (entries.length === 0) {
      setFailure({ message: 'Enter at least one score.' })
      return null
    }
    setFailure(null)
    try {
      const saved = await enterScores(enrolment.id, entries)
      queryClient.setQueryData(['results', 'detail', saved.id], saved)
      await refresh()
      return saved
    } catch (error) {
      setFailure(toApiError(error))
      return null
    }
  }

  const onSave = handleSubmit(async (values) => {
    if (await save(values)) toast.success('Scores saved as a draft')
  })

  // Project rule: when the scores cannot change, saying so waits until the user tries.
  const blockedTry = (action) => (event) => {
    if (notEditable) {
      event?.preventDefault()
      setBlockedTried(true)
      return
    }
    setBlockedTried(false)
    return action(event)
  }
  // URD FR-RES-02: missing scores or weights below 100% come back as the backend's 422 message, in the dialog.
  const trySubmit = handleSubmit(() => {
    setSubmitError(null)
    setConfirming(true)
  })

  const doSubmit = async () => {
    setBusy(true)
    try {
      const saved = isDirty || !result ? await save(getValues()) : result
      if (!saved) {
        setConfirming(false)
        return
      }
      const submitted = await submitResult(saved.id)
      queryClient.setQueryData(['results', 'detail', submitted.id], submitted)
      await refresh()
      setConfirming(false)
      toast.success(`Result submitted for approval: ${submitted.finalScore} (${submitted.grade})`)
    } catch (error) {
      setSubmitError(toApiError(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <Link
        to={`/offerings/${enrolment.offeringId}/enrolments`}
        className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline"
      >
        ← Offering enrolments
      </Link>
      <PageHeader
        title={`Scores: ${enrolment.farmerName}`}
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            {enrolment.programmeName}, {enrolment.seasonName} · {enrolment.membershipNumber}
            <ResultStatusBadge status={status} />
            {result && (
              <Link to={`/results/${result.id}`} className="font-semibold text-brand-700 hover:underline">
                Open the result
              </Link>
            )}
          </span>
        }
      />
      {inspectionList.inspections.length === 0 ? (
        <Alert tone="warning">
          This offering has no inspections yet. Define them on the offering&apos;s Inspections tab.
        </Alert>
      ) : (
        <form
          onSubmit={blockedTry(onSave)}
          noValidate
          className="max-w-2xl space-y-4 rounded-lg border border-line bg-white p-6"
        >
          {failure && <Alert tone="error">{failure.message}</Alert>}
          {blockedTried && notEditable && <Alert tone="warning">{notEditable}</Alert>}
          {inspectionList.inspections.map((i) => (
            <TextField
              key={i.id}
              label={i.name}
              type="number"
              step="0.01"
              min="0"
              max={i.maxScore}
              inputMode="decimal"
              hint={`0 to ${i.maxScore} · weight ${i.weight}%`}
              blockedReason={notEditable}
              error={errors[`s${i.id}`]?.message}
              {...register(`s${i.id}`, scoreRules(i.maxScore))}
            />
          ))}
          <div className="flex flex-wrap gap-2 pt-2">
            <Button type="submit" variant="secondary" loading={isSubmitting}>
              Save draft
            </Button>
            <Button onClick={blockedTry(trySubmit)}>Submit for approval</Button>
          </div>
        </form>
      )}
      <ConfirmDialog
        open={confirming}
        title={`Submit the result of ${enrolment.farmerName}?`}
        confirmLabel="Submit"
        busy={busy}
        onConfirm={doSubmit}
        onCancel={() => setConfirming(false)}
      >
        <div className="space-y-3">
          {submitError && <Alert tone="error">{submitError.message}</Alert>}
          <p>
            The District Agronomist will review it. You will not be able to change the scores unless it is returned to
            you, and the offering&apos;s inspections can no longer change.
          </p>
        </div>
      </ConfirmDialog>
    </>
  )
}
