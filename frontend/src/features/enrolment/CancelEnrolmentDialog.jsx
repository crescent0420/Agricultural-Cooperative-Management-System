import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextArea } from '../../components/ui/TextArea'
import { applyFieldErrors } from '../../utils/formErrors'

/**
 * Cancel an enrolment (URD FR-ENR-04, decision E1). The reason is optional for the farmer and required for the
 * Union Registrar, as in EnrolmentService.cancel. The place goes back to the offering. Refusals are shown here:
 * a farmer after the window has closed, anyone after the season has ended (422), already cancelled (409).
 */
export function CancelEnrolmentDialog({ enrolment, reasonRequired, onCancel, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { reason: '' } })

  const submit = handleSubmit(async ({ reason }) => {
    setFailure(null)
    try {
      await onCancel(reason.trim())
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['reason'])) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title="Cancel this enrolment?"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Keep enrolment
          </Button>
          <Button type="submit" form="cancel-enrolment-form" loading={isSubmitting}>
            Cancel enrolment
          </Button>
        </>
      }
    >
      <form id="cancel-enrolment-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <p className="text-sm">
          <strong>{enrolment.farmerName}</strong> will leave <strong>{enrolment.programmeName}</strong> (
          {enrolment.seasonName}), and the place goes back to the offering.
        </p>
        <TextArea
          label={reasonRequired ? 'Reason' : 'Reason (optional)'}
          maxLength={500}
          hint="At most 500 characters. Kept in the enrolment's record."
          error={errors.reason?.message}
          {...register('reason', {
            validate: (v) => !reasonRequired || v.trim().length > 0 || 'Give the reason for cancelling.',
            maxLength: { value: 500, message: 'Use at most 500 characters.' },
          })}
        />
      </form>
    </Dialog>
  )
}
