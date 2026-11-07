import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'

const FIELDS = ['name', 'maxScore', 'weight']

/**
 * Add or edit an inspection (URD FR-INS-02), with the checks of InspectionRequest. The backend also refuses
 * weights above 100% in total (422), a duplicate name, and changes to an inspection that has scores (409);
 * its message is shown here (decision R2). freeWeight is only a hint.
 */
export function InspectionDialog({ inspection, freeWeight, onSave, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      name: inspection?.name ?? '',
      maxScore: inspection?.maxScore ?? '',
      weight: inspection?.weight ?? '',
    },
  })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onSave({ name: values.name.trim(), maxScore: Number(values.maxScore), weight: Number(values.weight) })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, FIELDS)) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title={inspection ? `Edit ${inspection.name}` : 'Add an inspection'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="inspection-form" loading={isSubmitting}>
            {inspection ? 'Save' : 'Add'}
          </Button>
        </>
      }
    >
      <form id="inspection-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <TextField
          label="Name"
          placeholder="Soil and land preparation"
          error={errors.name?.message}
          {...register('name', {
            validate: (v) => v.trim().length > 0 || 'Enter a name.',
            maxLength: { value: 100, message: 'Use at most 100 characters.' },
          })}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Maximum score"
            type="number"
            step="0.01"
            inputMode="decimal"
            error={errors.maxScore?.message}
            {...register('maxScore', {
              required: 'Enter the maximum score.',
              validate: (v) =>
                (/^\d{1,4}(\.\d{1,2})?$/.test(String(v)) && Number(v) >= 0.01) ||
                'Enter at least 0.01, with at most 4 digits before the point and 2 after.',
            })}
          />
          <TextField
            label="Weight (%)"
            type="number"
            step="0.01"
            inputMode="decimal"
            hint={`${freeWeight}% of the 100% is still free.`}
            error={errors.weight?.message}
            {...register('weight', {
              required: 'Enter the weight.',
              validate: (v) =>
                (/^\d{1,3}(\.\d{1,2})?$/.test(String(v)) && Number(v) >= 0.01 && Number(v) <= 100) ||
                'Enter a percentage from 0.01 to 100, with at most 2 decimals.',
            })}
          />
        </div>
      </form>
    </Dialog>
  )
}
