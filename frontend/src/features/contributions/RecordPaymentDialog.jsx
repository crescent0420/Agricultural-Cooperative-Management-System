import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { newTransactionReference } from '../../api/payments'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { formatAmount } from '../../utils/format'
import { applyFieldErrors } from '../../utils/formErrors'
import { amountRules, METHOD_LABELS } from './contributionFields'

const NO_ANSWER =
  'No answer came back, so the payment may or may not have been recorded. Press Retry: it sends the same ' +
  'transaction reference, so the payment can never be recorded twice.'
const LOCKED =
  'This payment was already sent once with these details. To record a different amount or method, close this ' +
  'dialog and start a new payment.'

/**
 * Record a payment (URD FR-FIN-05). The transaction reference is made once, when the dialog opens, and every
 * retry sends it again, so a double click or a retry after a network failure can never create two payments:
 * the backend answers the repeat with the original ("Already received"). After an attempt whose answer was
 * lost, the amount and method lock (changing them would make the retry a different payment: 409).
 */
export function RecordPaymentDialog({ contribution, onRecord, onClose }) {
  const [reference] = useState(() => newTransactionReference(contribution.id))
  const [failure, setFailure] = useState(null)
  const [sentWithoutAnswer, setSentWithoutAnswer] = useState(false)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { amount: '', method: '' } })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onRecord({
        transactionReference: reference,
        contributionId: contribution.id,
        amount: Number(values.amount),
        method: values.method,
      })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      // No answer, or a gateway/server failure (a proxy in front of the backend turns a lost answer into 502/504,
      // the Vite dev proxy into 500): the payment may or may not exist, so the only safe step is the same request.
      if (apiError.status === 0 || apiError.status >= 500) {
        setSentWithoutAnswer(true)
        setFailure({ message: NO_ANSWER })
      } else if (!applyFieldErrors(apiError, setError, ['amount', 'method'])) {
        setFailure(apiError)
      }
    }
  })

  return (
    <Dialog
      open
      title="Record a payment"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Close
          </Button>
          <Button type="submit" form="payment-form" loading={isSubmitting}>
            {sentWithoutAnswer ? 'Retry' : 'Save'}
          </Button>
        </>
      }
    >
      <form id="payment-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <p className="text-sm">
          {contribution.farmerName}, {contribution.seasonName}:{' '}
          <strong>{formatAmount(contribution.outstanding)}</strong> outstanding, of which{' '}
          {formatAmount(contribution.pendingAmount)} is waiting for confirmation.
        </p>
        <TextField
          label="Amount"
          inputMode="decimal"
          blockedReason={sentWithoutAnswer ? LOCKED : undefined}
          error={errors.amount?.message}
          {...register('amount', amountRules('the amount'))}
        />
        <Select
          label="Method"
          blockedReason={sentWithoutAnswer ? LOCKED : undefined}
          error={errors.method?.message}
          {...register('method', { required: 'Choose how it was paid.' })}
        >
          <option value="">Choose…</option>
          {Object.entries(METHOD_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        <p className="text-xs text-muted">
          Transaction reference <span className="font-mono">{reference}</span>. It counts only after confirmation.
        </p>
      </form>
    </Dialog>
  )
}
