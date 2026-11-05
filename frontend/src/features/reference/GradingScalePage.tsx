import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { ApiError, toApiError } from '../../api/errors'
import { getGradingScale, replaceGradingScale } from '../../api/reference'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { TextField } from '../../components/ui/TextField'
import type { GradingBand } from '../../types/reference'
import { applyFieldErrors } from '../../utils/formErrors'

interface ScaleForm {
  bands: { grade: string; minScore: number; description: string }[]
}

const MAX_BANDS = 10
const byMinScoreDesc = (a: GradingBand, b: GradingBand) => b.minScore - a.minScore

/**
 * /grading-scale (URD FR-REF-07). Everyone reads; the Union Registrar replaces the whole scale.
 * Backend rules mirrored in the form: 1-10 bands, unique grades (max 5 characters), unique minimum
 * scores from 0 to 100, and one band starting at 0 so every final score gets a grade.
 */
export function GradingScalePage() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('GRADING_SCALE_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [failure, setFailure] = useState<ApiError | null>(null)

  const scale = useQuery({ queryKey: ['grading-scale'], queryFn: getGradingScale })
  const bands = [...(scale.data ?? [])].sort(byMinScoreDesc)

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<ScaleForm>({ defaultValues: { bands: [] } })
  const rows = useFieldArray({ control, name: 'bands' })

  const save = useMutation({
    mutationFn: (values: GradingBand[]) => replaceGradingScale(values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['grading-scale'] })
      setEditing(false)
      toast.success('Grading scale saved')
    },
  })

  const startEditing = () => {
    reset({ bands: bands.map((b) => ({ grade: b.grade, minScore: b.minScore, description: b.description ?? '' })) })
    setFailure(null)
    setEditing(true)
  }

  const submit = handleSubmit(async ({ bands: values }) => {
    setFailure(null)
    if (!values.some((b) => b.minScore === 0)) {
      setFailure(new ApiError(400, 'One band must start at 0, so that every score gets a grade.'))
      return
    }
    try {
      await save.mutateAsync(
        values.map((b) => ({ grade: b.grade.trim(), minScore: b.minScore, description: b.description.trim() || null })),
      )
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, ['bands'])) setFailure(apiError)
    }
  })

  return (
    <>
      <PageHeader
        title="Grading scale"
        description="How a final score (0 to 100) becomes a quality grade when results are published."
        actions={canManage && !editing && <Button onClick={startEditing} disabled={scale.isPending}>Edit scale</Button>}
      />

      {!editing && (
        <DataTable
          caption="Grading scale"
          columns={[
            { header: 'Grade', cell: (b: GradingBand) => <span className="font-semibold">{b.grade}</span> },
            { header: 'From score', cell: (b: GradingBand) => b.minScore },
            { header: 'Description', cell: (b: GradingBand) => b.description ?? '—' },
          ]}
          rows={bands}
          rowKey={(b) => b.grade}
          loading={scale.isPending}
          error={scale.error ? toApiError(scale.error) : null}
          emptyMessage="No grading scale has been defined yet."
        />
      )}

      {editing && (
        <form onSubmit={submit} noValidate className="space-y-4">
          {failure && <Alert tone="error">{failure.message}</Alert>}
          {errors.bands?.root?.message && <Alert tone="error">{errors.bands.root.message}</Alert>}
          <div className="space-y-3">
            {rows.fields.map((field, index) => (
              <fieldset key={field.id} className="grid gap-3 rounded-lg border border-line bg-white p-4 sm:grid-cols-[6rem_8rem_1fr_auto] sm:items-start">
                <legend className="sr-only">Band {index + 1}</legend>
                <TextField
                  label="Grade"
                  error={errors.bands?.[index]?.grade?.message}
                  {...register(`bands.${index}.grade`, {
                    validate: (value) => {
                      const grade = value.trim()
                      if (!grade) return 'Required.'
                      if (grade.length > 5) return 'Max 5 characters.'
                      const same = getValues('bands').filter((b) => b.grade.trim().toLowerCase() === grade.toLowerCase())
                      return same.length === 1 || 'Used twice.'
                    },
                  })}
                />
                <TextField
                  label="From score"
                  type="number"
                  step="0.01"
                  min={0}
                  max={100}
                  error={errors.bands?.[index]?.minScore?.message}
                  {...register(`bands.${index}.minScore`, {
                    valueAsNumber: true,
                    validate: (value) => {
                      if (Number.isNaN(value)) return 'Required.'
                      if (value < 0 || value > 100) return '0 to 100.'
                      const same = getValues('bands').filter((b) => b.minScore === value)
                      return same.length === 1 || 'Used twice.'
                    },
                  })}
                />
                <TextField
                  label="Description (optional)"
                  error={errors.bands?.[index]?.description?.message}
                  {...register(`bands.${index}.description`, {
                    maxLength: { value: 100, message: 'Max 100 characters.' },
                  })}
                />
                <Button
                  variant="link"
                  className="sm:mt-8"
                  disabled={rows.fields.length === 1}
                  onClick={() => rows.remove(index)}
                  aria-label={`Remove band ${index + 1}`}
                >
                  Remove
                </Button>
              </fieldset>
            ))}
          </div>
          <div className="flex flex-wrap justify-between gap-3">
            <Button
              variant="secondary"
              disabled={rows.fields.length >= MAX_BANDS}
              onClick={() => rows.append({ grade: '', minScore: Number.NaN, description: '' })}
            >
              Add band
            </Button>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => setEditing(false)} disabled={isSubmitting}>
                Cancel
              </Button>
              <Button type="submit" loading={isSubmitting}>
                Save scale
              </Button>
            </div>
          </div>
          <p className="text-xs text-muted">Up to {MAX_BANDS} bands. Saving replaces the whole scale.</p>
        </form>
      )}
    </>
  )
}
