import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { TextArea } from '../../components/ui/TextArea'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { scoreRules, scoreText } from './resultFields'

/**
 * Correct one score of a PUBLISHED result (URD FR-RES-05): inspection, new score, mandatory reason.
 * The backend refuses the same score (400) and keeps the original; its message is shown here.
 */
export function CorrectionDialog({ result, onCorrect, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { inspectionId: '', correctedScore: '', reason: '' } })
  const chosenId = useWatch({ control, name: 'inspectionId' })
  const chosen = result.scores.find((s) => String(s.inspectionId) === chosenId)

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onCorrect({
        inspectionId: Number(values.inspectionId),
        correctedScore: Number(values.correctedScore),
        reason: values.reason.trim(),
      })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['inspectionId', 'correctedScore', 'reason'])) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title="Correct a published score"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="correction-form" loading={isSubmitting}>
            Correct
          </Button>
        </>
      }
    >
      <form id="correction-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <Select
          label="Inspection"
          error={errors.inspectionId?.message}
          {...register('inspectionId', { required: 'Choose the inspection to correct.' })}
        >
          <option value="">Choose an inspection…</option>
          {result.scores.map((s) => (
            <option key={s.inspectionId} value={s.inspectionId}>
              {s.inspectionName} (now {scoreText(s.currentScore)} of {s.maxScore})
            </option>
          ))}
        </Select>
        <TextField
          label="Corrected score"
          type="number"
          step="0.01"
          min="0"
          inputMode="decimal"
          hint={chosen ? `0 to ${chosen.maxScore}; now ${scoreText(chosen.currentScore)}.` : undefined}
          error={errors.correctedScore?.message}
          {...register('correctedScore', scoreRules(chosen?.maxScore ?? 9999.99, { required: true }))}
        />
        <TextArea
          label="Reason"
          maxLength={500}
          hint="Kept in the correction history with the original score."
          error={errors.reason?.message}
          {...register('reason', {
            validate: (v) => v.trim().length > 0 || 'Give the reason for the correction.',
            maxLength: { value: 500, message: 'Use at most 500 characters.' },
          })}
        />
      </form>
    </Dialog>
  )
}
