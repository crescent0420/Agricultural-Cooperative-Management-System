import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { toApiError } from '../../api/errors'
import { registerFarmer } from '../../api/farmers'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { useCooperativeOptions, useDistrictOptions } from '../reference/useReferenceOptions'
import { fullNameRules, nationalIdRules, phoneOrNull, phoneRules } from './farmerRules'

const FIELDS = ['fullName', 'nationalId', 'phoneNumber', 'cooperativeId']

/**
 * /farmers/new: the Union Registrar registers a farmer (URD FR-FAR-03).
 * The district is chosen first only to shorten the cooperative list; the backend stores the cooperative.
 */
export function RegisterFarmerPage() {
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
  } = useForm({ defaultValues: { fullName: '', nationalId: '', phoneNumber: '', districtId: '', cooperativeId: '' } })

  const districtId = useWatch({ control, name: 'districtId' })
  const districts = useDistrictOptions()
  const cooperatives = useCooperativeOptions(districtId ? Number(districtId) : null)

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      const farmer = await registerFarmer({
        fullName: values.fullName.trim(),
        nationalId: values.nationalId,
        phoneNumber: phoneOrNull(values.phoneNumber),
        cooperativeId: Number(values.cooperativeId),
      })
      await queryClient.invalidateQueries({ queryKey: ['farmers'] })
      toast.success(`${farmer.fullName} registered with membership number ${farmer.membershipNumber}`)
      navigate(`/farmers/${farmer.id}`)
    } catch (error) {
      const apiError = toApiError(error)
      if (apiError.status === 409) {
        // The only conflict on registration: this national ID is already registered.
        setError('nationalId', { message: apiError.message })
      } else if (!applyFieldErrors(apiError, setError, FIELDS)) {
        setFailure(apiError)
      }
    }
  })

  return (
    <>
      <Link to="/farmers" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Farmers
      </Link>
      <PageHeader title="Register farmer" description="The membership number is created by the system when you save." />
      <form onSubmit={submit} noValidate className="max-w-2xl space-y-4 rounded-lg border border-line bg-white p-6">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <TextField
          label="Full name"
          autoComplete="off"
          error={errors.fullName?.message}
          {...register('fullName', fullNameRules)}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="National ID"
            inputMode="numeric"
            maxLength={16}
            hint="16 digits"
            error={errors.nationalId?.message}
            {...register('nationalId', nationalIdRules)}
          />
          <TextField
            label="Phone number (optional)"
            type="tel"
            placeholder="+250788123456"
            error={errors.phoneNumber?.message}
            {...register('phoneNumber', phoneRules)}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Select
            label="District"
            error={errors.districtId?.message}
            {...register('districtId', {
              required: 'Choose a district.',
              onChange: () => setValue('cooperativeId', ''),
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
            label="Cooperative"
            disabled={!districtId}
            error={errors.cooperativeId?.message}
            hint={
              districtId && cooperatives.data?.content.length === 0
                ? 'This district has no cooperatives yet (Reference data).'
                : undefined
            }
            {...register('cooperativeId', { required: 'Choose a cooperative.' })}
          >
            <option value="">{districtId ? 'Choose a cooperative…' : 'Choose a district first'}</option>
            {cooperatives.data?.content.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex gap-2 pt-2">
          <Button type="submit" loading={isSubmitting}>
            Register farmer
          </Button>
          <Button variant="secondary" onClick={() => navigate('/farmers')} disabled={isSubmitting}>
            Cancel
          </Button>
        </div>
      </form>
    </>
  )
}
