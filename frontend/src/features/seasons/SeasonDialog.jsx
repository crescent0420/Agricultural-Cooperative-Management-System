import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'

const FIELDS = ['name', 'startsOn', 'endsOn', 'maxLandHectares', 'distributionDate']

/**
 * Create or edit a season (URD FR-SEA-02). The same checks as SeasonRequest and SeasonService.validateDates:
 * the end after the start, the distribution date inside the season, land above 0 with at most 2 decimals.
 * "YYYY-MM-DD" strings compare in date order, so the date checks compare the strings directly.
 */
export function SeasonDialog({ season, onSave, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      name: season?.name ?? '',
      startsOn: season?.startsOn ?? '',
      endsOn: season?.endsOn ?? '',
      maxLandHectares: season?.maxLandHectares ?? '',
      distributionDate: season?.distributionDate ?? '',
    },
  })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onSave({ ...values, name: values.name.trim(), maxLandHectares: Number(values.maxLandHectares) })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, FIELDS)) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title={season ? `Edit ${season.name}` : 'New season'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="season-form" loading={isSubmitting}>
            {season ? 'Save' : 'Create'}
          </Button>
        </>
      }
    >
      <form id="season-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <TextField
          label="Name"
          placeholder="2027A"
          error={errors.name?.message}
          {...register('name', {
            validate: (v) => v.trim().length > 0 || 'Enter a name.',
            maxLength: { value: 100, message: 'Use at most 100 characters.' },
          })}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Starts on"
            type="date"
            error={errors.startsOn?.message}
            {...register('startsOn', { required: 'Enter the start date.' })}
          />
          <TextField
            label="Ends on"
            type="date"
            error={errors.endsOn?.message}
            {...register('endsOn', {
              required: 'Enter the end date.',
              validate: (v) =>
                !getValues('startsOn') || v > getValues('startsOn') || 'The end must be after the start.',
            })}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Maximum land per farmer (ha)"
            type="number"
            step="0.01"
            min="0.01"
            inputMode="decimal"
            error={errors.maxLandHectares?.message}
            {...register('maxLandHectares', {
              required: 'Enter the land limit.',
              validate: (v) =>
                (/^\d{1,6}(\.\d{1,2})?$/.test(String(v)) && Number(v) > 0) ||
                'Enter more than 0, with at most 2 decimals.',
            })}
          />
          <TextField
            label="Distribution date"
            type="date"
            hint="When inputs are handed out; inside the season."
            error={errors.distributionDate?.message}
            {...register('distributionDate', {
              required: 'Enter the distribution date.',
              validate: (v) => {
                const { startsOn, endsOn } = getValues()
                if (startsOn && v < startsOn) return 'The distribution date must be inside the season.'
                if (endsOn && v > endsOn) return 'The distribution date must be inside the season.'
                return true
              },
            })}
          />
        </div>
      </form>
    </Dialog>
  )
}
