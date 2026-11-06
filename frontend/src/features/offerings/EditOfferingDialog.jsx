import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { Spinner } from '../../components/ui/Spinner'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { hectaresRules, maxParticipantsRules } from './offeringRules'
import { useFieldOfficers } from './useFieldOfficers'

/**
 * Edit an offering (URD FR-OFF-04): Field Officer, hectares, maximum. The programme and season never change.
 * S3: hectares are locked once a farmer has enrolled; the maximum may not go below the enrolled count
 * (the backend answers 409 with the reason, shown here).
 */
export function EditOfferingDialog({ offering, onSave, onClose }) {
  const [failure, setFailure] = useState(null)
  const officers = useFieldOfficers(offering.districtId)
  const hectaresLocked = offering.enrolledCount > 0
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      fieldOfficerId: String(offering.fieldOfficerId),
      hectaresPerParticipant: offering.hectaresPerParticipant,
      maxParticipants: offering.maxParticipants,
    },
  })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onSave({
        fieldOfficerId: Number(values.fieldOfficerId),
        // A locked field is sent unchanged, so the backend sees no change of hectares.
        hectaresPerParticipant: hectaresLocked
          ? offering.hectaresPerParticipant
          : Number(values.hectaresPerParticipant),
        maxParticipants: Number(values.maxParticipants),
      })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['fieldOfficerId', 'hectaresPerParticipant', 'maxParticipants'])) {
        setFailure(apiError)
      }
    }
  })

  // The current officer stays selectable even when no longer in the G8 list (e.g. disabled since).
  const listed = officers.data ?? []
  const options = listed.some((o) => o.id === offering.fieldOfficerId)
    ? listed
    : [{ id: offering.fieldOfficerId, username: `${offering.fieldOfficerUsername} (no longer available)` }, ...listed]

  return (
    <Dialog
      open
      title={`Edit ${offering.programmeName}, ${offering.seasonName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="edit-offering-form" loading={isSubmitting}>
            Save
          </Button>
        </>
      }
    >
      {officers.isPending ? (
        <Spinner label="Loading the Field Officers" />
      ) : (
        <form id="edit-offering-form" onSubmit={submit} noValidate className="space-y-4">
          {failure && <Alert tone="error">{failure.message}</Alert>}
          <Select
            label="Field Officer"
            error={errors.fieldOfficerId?.message}
            hint={`Enabled Field Officers of ${offering.districtName}.`}
            {...register('fieldOfficerId', { required: 'Choose a Field Officer.' })}
          >
            {options.map((o) => (
              <option key={o.id} value={o.id}>
                {o.username}
              </option>
            ))}
          </Select>
          <TextField
            label="Hectares per participant"
            type="number"
            step="0.01"
            inputMode="decimal"
            readOnly={hectaresLocked}
            hint={
              hectaresLocked
                ? `Cannot change: ${offering.enrolledCount} farmer(s) already enrolled with this land size.`
                : undefined
            }
            error={errors.hectaresPerParticipant?.message}
            {...register('hectaresPerParticipant', hectaresLocked ? {} : hectaresRules)}
          />
          <TextField
            label="Maximum participants"
            type="number"
            step="1"
            inputMode="numeric"
            hint={`${offering.enrolledCount} already enrolled: the maximum cannot go below that.`}
            error={errors.maxParticipants?.message}
            {...register('maxParticipants', maxParticipantsRules)}
          />
        </form>
      )}
    </Dialog>
  )
}
