import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { fullNameRules, phoneOrNull, phoneRules } from './farmerRules'

/**
 * Edit a farmer's name and phone (URD FR-FAR-04). The national ID and the cooperative cannot be changed:
 * PUT /api/farmers/{id} accepts only these two fields.
 */
export function EditFarmerDialog({ farmer, onSave, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { fullName: farmer.fullName, phoneNumber: farmer.phoneNumber ?? '' } })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onSave({ fullName: values.fullName.trim(), phoneNumber: phoneOrNull(values.phoneNumber) })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['fullName', 'phoneNumber'])) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title={`Edit ${farmer.fullName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="edit-farmer-form" loading={isSubmitting}>
            Save
          </Button>
        </>
      }
    >
      <form id="edit-farmer-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <TextField label="Full name" error={errors.fullName?.message} {...register('fullName', fullNameRules)} />
        <TextField
          label="Phone number (optional)"
          type="tel"
          error={errors.phoneNumber?.message}
          {...register('phoneNumber', phoneRules)}
        />
        <p className="text-xs text-muted">The national ID and the cooperative cannot be changed after registration.</p>
      </form>
    </Dialog>
  )
}
