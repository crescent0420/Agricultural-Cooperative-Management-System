import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { todayIso } from '../../utils/format'

/**
 * Record a completed training (URD FR-FAR-07). The date may not be in the future (@PastOrPresent);
 * recording the same training twice answers 409, shown here.
 */
export function RecordTrainingDialog({ trainings, onSave, onClose }) {
  const [failure, setFailure] = useState(null)
  const today = todayIso()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { trainingId: '', completedOn: today } })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onSave(Number(values.trainingId), values.completedOn)
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['trainingId', 'completedOn'])) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title="Record a completed training"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="record-training-form" loading={isSubmitting}>
            Record
          </Button>
        </>
      }
    >
      <form id="record-training-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <Select
          label="Training"
          error={errors.trainingId?.message}
          {...register('trainingId', { required: 'Choose a training.' })}
        >
          <option value="">Choose a training…</option>
          {trainings.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </Select>
        <TextField
          label="Completed on"
          type="date"
          max={today}
          error={errors.completedOn?.message}
          {...register('completedOn', {
            required: 'Enter the completion date.',
            // "YYYY-MM-DD" strings compare in date order.
            validate: (value) => value <= today || 'The completion date cannot be in the future.',
          })}
        />
      </form>
    </Dialog>
  )
}
