import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { TextArea } from '../../components/ui/TextArea'
import { TextField } from '../../components/ui/TextField'
import { formatAmount } from '../../utils/format'
import { applyFieldErrors } from '../../utils/formErrors'
import { amountRules } from './contributionFields'

/**
 * FR-FIN-03: change the amount owed, with a mandatory reason; the change joins the adjustment history.
 * The backend refuses the same amount (400) and an amount below what is already confirmed (422, P4).
 */
export function AdjustAmountDialog({ contribution, onAdjust, onClose }) {
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { newAmountDue: '', reason: '' } })
  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onAdjust({ newAmountDue: Number(values.newAmountDue), reason: values.reason.trim() })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['newAmountDue', 'reason'])) setFailure(apiError)
    }
  })
  return (
    <Dialog
      open
      title="Adjust the amount owed"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="adjust-form" loading={isSubmitting}>
            Adjust
          </Button>
        </>
      }
    >
      <form id="adjust-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <TextField
          label="New amount due"
          inputMode="decimal"
          hint={`Now ${formatAmount(contribution.amountDue)}; ${formatAmount(contribution.confirmedPaid)} already confirmed.`}
          error={errors.newAmountDue?.message}
          {...register('newAmountDue', amountRules('the new amount'))}
        />
        <TextArea
          label="Reason"
          maxLength={500}
          hint="Kept in the adjustment history with the old and new amounts."
          error={errors.reason?.message}
          {...register('reason', {
            validate: (v) => v.trim().length > 0 || 'Give the reason for the change.',
            maxLength: { value: 500, message: 'Use at most 500 characters.' },
          })}
        />
      </form>
    </Dialog>
  )
}
