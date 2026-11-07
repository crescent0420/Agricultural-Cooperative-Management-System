import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { useSeasonOptions } from '../seasons/useSeasonOptions'
import { amountRules } from './contributionFields'
import { FarmerPicker } from './FarmerPicker'

/** FR-FIN-02: a new contribution obligation. One per farmer and season: a second answers 409, shown here. */
export function NewContributionDialog({ onCreate, onClose }) {
  const [failure, setFailure] = useState(null)
  const seasons = useSeasonOptions()
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { farmer: null, seasonId: '', amountDue: '' } })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onCreate({
        farmerId: values.farmer.id,
        seasonId: Number(values.seasonId),
        amountDue: Number(values.amountDue),
      })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['seasonId', 'amountDue'])) setFailure(apiError)
    }
  })

  return (
    <Dialog
      open
      title="New contribution"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="contribution-form" loading={isSubmitting}>
            Create
          </Button>
        </>
      }
    >
      <form id="contribution-form" onSubmit={submit} noValidate className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <Controller
          name="farmer"
          control={control}
          rules={{ validate: (v) => v != null || 'Find and choose the farmer.' }}
          render={({ field, fieldState }) => (
            <FarmerPicker value={field.value} onChange={field.onChange} error={fieldState.error?.message} />
          )}
        />
        <Select
          label="Season"
          error={errors.seasonId?.message}
          {...register('seasonId', { required: 'Choose a season.' })}
        >
          <option value="">Choose a season…</option>
          {seasons.data?.content.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </Select>
        <TextField
          label="Amount due"
          inputMode="decimal"
          hint="Seed, fertiliser and other inputs for the season."
          error={errors.amountDue?.message}
          {...register('amountDue', amountRules('the amount due'))}
        />
      </form>
    </Dialog>
  )
}
