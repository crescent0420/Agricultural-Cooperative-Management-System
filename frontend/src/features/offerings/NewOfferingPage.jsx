import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { toApiError } from '../../api/errors'
import { createOffering } from '../../api/offerings'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { useDistrictOptions, useProgrammeOptions } from '../reference/useReferenceOptions'
import { useSeasonOptions } from '../seasons/useSeasonOptions'
import { hectaresRules, maxParticipantsRules } from './offeringRules'
import { useFieldOfficers } from './useFieldOfficers'

const FIELDS = ['programmeId', 'seasonId', 'fieldOfficerId', 'hectaresPerParticipant', 'maxParticipants']

/**
 * /offerings/new: the Union Registrar offers a crop programme in a season (URD FR-OFF-03).
 * The district comes first: it decides the programmes and the Field Officers (G8) that can be chosen.
 */
export function NewOfferingPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    setValue,
    control,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      districtId: '',
      programmeId: '',
      seasonId: '',
      fieldOfficerId: '',
      hectaresPerParticipant: '',
      maxParticipants: '',
    },
  })
  const [districtValue, seasonValue] = useWatch({ control, name: ['districtId', 'seasonId'] })
  const districtId = districtValue ? Number(districtValue) : null
  const districts = useDistrictOptions()
  const programmes = useProgrammeOptions(districtId)
  const officers = useFieldOfficers(districtId)
  const seasons = useSeasonOptions()
  const season = seasons.data?.content.find((s) => String(s.id) === seasonValue)

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      const offering = await createOffering({
        programmeId: Number(values.programmeId),
        seasonId: Number(values.seasonId),
        fieldOfficerId: Number(values.fieldOfficerId),
        hectaresPerParticipant: Number(values.hectaresPerParticipant),
        maxParticipants: Number(values.maxParticipants),
      })
      await queryClient.invalidateQueries({ queryKey: ['offerings'] })
      toast.success(`${offering.programmeName} offered in ${offering.seasonName}`)
      navigate(`/offerings/${offering.id}`)
    } catch (error) {
      const apiError = toApiError(error)
      if (!applyFieldErrors(apiError, setError, FIELDS)) setFailure(apiError)
    }
  })

  const noOfficers = districtId != null && officers.data?.length === 0

  return (
    <>
      <Link to="/offerings" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Offerings
      </Link>
      <PageHeader title="New offering" description="Offer a crop programme in a season, with its Field Officer." />
      <form onSubmit={submit} noValidate className="max-w-2xl space-y-4 rounded-lg border border-line bg-white p-6">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Select
            label="District"
            error={errors.districtId?.message}
            {...register('districtId', {
              required: 'Choose a district.',
              onChange: () => {
                setValue('programmeId', '')
                setValue('fieldOfficerId', '')
              },
            })}
          >
            <option value="">Choose a district…</option>
            {districts.data?.content.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </Select>
          <Select
            label="Crop programme"
            error={errors.programmeId?.message}
            blockedReason={
              districtId == null
                ? 'Choose a district first.'
                : programmes.data?.content.length === 0
                  ? 'This district has no crop programmes yet (Reference data).'
                  : undefined
            }
            {...register('programmeId', { required: 'Choose a crop programme.' })}
          >
            <option value="">Choose a programme…</option>
            {programmes.data?.content.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
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
          <Select
            label="Field Officer"
            error={errors.fieldOfficerId?.message}
            blockedReason={
              districtId == null
                ? 'Choose a district first.'
                : noOfficers
                  ? 'This district has no enabled Field Officer. Ask the administrator to create one.'
                  : undefined
            }
            hint={districtId != null && !noOfficers ? 'Enabled Field Officers of the chosen district.' : undefined}
            {...register('fieldOfficerId', { required: 'Choose a Field Officer.' })}
          >
            <option value="">Choose a Field Officer…</option>
            {officers.data?.map((o) => (
              <option key={o.id} value={o.id}>
                {o.username}
              </option>
            ))}
          </Select>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Hectares per participant"
            type="number"
            step="0.01"
            min="0.01"
            inputMode="decimal"
            hint={season ? `${season.name} allows at most ${season.maxLandHectares} ha per farmer.` : undefined}
            error={errors.hectaresPerParticipant?.message}
            {...register('hectaresPerParticipant', hectaresRules)}
          />
          <TextField
            label="Maximum participants"
            type="number"
            step="1"
            min="1"
            inputMode="numeric"
            hint="Set by the seed and fertiliser stock available."
            error={errors.maxParticipants?.message}
            {...register('maxParticipants', maxParticipantsRules)}
          />
        </div>
        <div className="flex gap-2 pt-2">
          <Button type="submit" loading={isSubmitting}>
            Create offering
          </Button>
          <Button variant="secondary" onClick={() => navigate('/offerings')} disabled={isSubmitting}>
            Cancel
          </Button>
        </div>
      </form>
    </>
  )
}
