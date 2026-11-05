import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toApiError, type ApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import type { District } from '../../types/reference'
import { applyFieldErrors } from '../../utils/formErrors'

export interface NameFormValues {
  name: string
  districtId: string
}

interface NameDialogProps {
  title: string
  submitLabel: string
  /** Backend limit: 150 for cooperatives, programmes and trainings; 100 for districts. */
  maxLength: number
  initial?: Partial<NameFormValues>
  /** A caution shown above the form, e.g. for official district names (URD gap G11). */
  warning?: string
  /** Shown only when creating a cooperative or programme (a rename never changes the district). */
  districts?: District[]
  onSubmit: (values: { name: string; districtId: number | null }) => Promise<unknown>
  onClose: () => void
}

/** Create or rename dialog shared by districts, cooperatives, crop programmes and trainings. */
export function NameDialog({ title, submitLabel, maxLength, initial, warning, districts, onSubmit, onClose }: NameDialogProps) {
  const [failure, setFailure] = useState<ApiError | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<NameFormValues>({ defaultValues: { name: initial?.name ?? '', districtId: initial?.districtId ?? '' } })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      await onSubmit({ name: values.name.trim(), districtId: values.districtId ? Number(values.districtId) : null })
      onClose()
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['name', 'districtId'])) setFailure(apiError)
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
          <Button type="submit" form="name-dialog-form" loading={isSubmitting}>
            {submitLabel}
          </Button>
        </>
      }
    >
      <form id="name-dialog-form" onSubmit={submit} noValidate className="space-y-4">
        {warning && <Alert tone="warning">{warning}</Alert>}
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <TextField
          label="Name"
          error={errors.name?.message}
          {...register('name', {
            validate: (value) => value.trim().length > 0 || 'Enter a name.',
            maxLength: { value: maxLength, message: `Use at most ${maxLength} characters.` },
          })}
        />
        {districts && (
          <Select
            label="District"
            error={errors.districtId?.message}
            {...register('districtId', { required: 'Choose a district.' })}
          >
            <option value="">Choose a district…</option>
            {districts.map((district) => (
              <option key={district.id} value={district.id}>
                {district.name}
              </option>
            ))}
          </Select>
        )}
      </form>
    </Dialog>
  )
}
