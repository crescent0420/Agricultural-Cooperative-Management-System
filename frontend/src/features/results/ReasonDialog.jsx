import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextArea } from '../../components/ui/TextArea'

/** A dialog that asks for a required reason (max 500) before an action, e.g. returning a result to draft. */
export function ReasonDialog({ title, intro, confirmLabel, onConfirm, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { reason: '' } })
  const submit = handleSubmit(async ({ reason }) => {
    setFailure(null)
    try {
      await onConfirm(reason.trim())
      onClose()
    } catch (error) {
      setFailure(toApiError(error))
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
          <Button type="submit" form="reason-form" loading={isSubmitting}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <form id="reason-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        {intro && <p className="text-sm">{intro}</p>}
        <TextArea
          label="Reason"
          maxLength={500}
          hint="At most 500 characters. Kept in the record."
          error={errors.reason?.message}
          {...register('reason', {
            validate: (v) => v.trim().length > 0 || 'Give the reason.',
            maxLength: { value: 500, message: 'Use at most 500 characters.' },
          })}
        />
      </form>
    </Dialog>
  )
}
