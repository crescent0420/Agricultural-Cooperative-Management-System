import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { changeUserRole } from '../../api/users'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { conflictField, scopeFields } from './accountRules'
import { AccountRoleFields } from './AccountRoleFields'

/**
 * Change a user's role and what it is attached to (FR-ADM-03). For a farmer account without a farmer record
 * this is "Link to a farmer record": the role stays Farmer and only the record is chosen.
 */
export function ChangeRoleDialog({ user, title, onClose, onChanged }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      role: user.role,
      districtId: user.districtId != null ? String(user.districtId) : '',
      // The current record is kept unless another is picked; its name is not readable by the admin (A2).
      farmer:
        user.farmerId != null
          ? { id: user.farmerId, fullName: `Farmer #${user.farmerId}`, membershipNumber: '' }
          : null,
    },
  })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      const updated = await changeUserRole(user.id, {
        role: values.role,
        ...scopeFields(values.role, values.districtId, values.farmer),
      })
      onChanged(updated)
    } catch (error) {
      const apiError = toApiError(error)
      if (apiError.status === 409 && conflictField(apiError.message) === 'farmer') {
        setError('farmer', { message: apiError.message })
      } else {
        setFailure(apiError)
      }
    }
  })

  return (
    <Dialog
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button onClick={submit} loading={isSubmitting}>
            Save
          </Button>
        </>
      }
    >
      <form onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <AccountRoleFields register={register} control={control} errors={errors} />
        <p className="text-sm text-muted">The change applies from {user.username}&apos;s next request.</p>
      </form>
    </Dialog>
  )
}
