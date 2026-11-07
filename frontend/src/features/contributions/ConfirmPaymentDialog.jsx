import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextField } from '../../components/ui/TextField'
import { formatAmount } from '../../utils/format'
import { applyFieldErrors } from '../../utils/formErrors'
import { METHOD_LABELS } from './contributionFields'

/** FR-FIN-06: confirm a PENDING payment with the provider's reference; from then on it counts. */
export function ConfirmPaymentDialog({ payment, onConfirm, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { providerReference: '' } })
  const submit = handleSubmit(async ({ providerReference }) => {
    setFailure(null)
    try {
      await onConfirm(providerReference.trim())
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['providerReference'])) setFailure(apiError)
    }
  })
  return (
    <Dialog
      open
      title="Confirm this payment?"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="confirm-payment-form" loading={isSubmitting}>
            Confirm payment
          </Button>
        </>
      }
    >
      <form id="confirm-payment-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <p className="text-sm">
          <strong>{formatAmount(payment.amount)}</strong> by {METHOD_LABELS[payment.method]}, reference{' '}
          <span className="font-mono">{payment.transactionReference}</span>. Once confirmed it counts towards the
          balance, and it cannot be undone.
        </p>
        <TextField
          label="Provider reference"
          hint="The mobile-money or bank reference that proves the money arrived."
          error={errors.providerReference?.message}
          {...register('providerReference', {
            validate: (v) => v.trim().length > 0 || 'Enter the provider reference.',
            maxLength: { value: 100, message: 'Use at most 100 characters.' },
          })}
        />
      </form>
    </Dialog>
  )
}
