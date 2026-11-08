import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { toApiError } from '../../api/errors'
import { createUser } from '../../api/users'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { TextField } from '../../components/ui/TextField'
import { applyFieldErrors } from '../../utils/formErrors'
import { conflictField, EMAIL_RULES, scopeFields, USERNAME_RULES } from './accountRules'
import { AccountRoleFields } from './AccountRoleFields'

const FIELDS = ['username', 'email', 'role', 'districtId', 'farmerId']

/** /admin/users/new: the administrator creates an account; the backend emails a one-time password (FR-ADM-02). */
export function NewUserPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { username: '', email: '', role: '', districtId: '', farmer: null } })

  const submit = handleSubmit(async (values) => {
    setFailure(null)
    try {
      const user = await createUser({
        username: values.username.trim(),
        email: values.email.trim(),
        role: values.role,
        ...scopeFields(values.role, values.districtId, values.farmer),
      })
      await queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success(`Account ${user.username} created. A one-time password was emailed to ${user.email}.`)
      navigate(`/admin/users/${user.id}`)
    } catch (error) {
      const apiError = toApiError(error)
      const field = apiError.status === 409 ? conflictField(apiError.message) : null
      if (field) setError(field, { message: apiError.message })
      else if (!applyFieldErrors(apiError, setError, FIELDS)) setFailure(apiError)
    }
  })

  return (
    <>
      <Link to="/admin/users" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Users
      </Link>
      <PageHeader
        title="Create account"
        description="The system generates a one-time password and emails it; the user must change it at first login."
      />
      <form onSubmit={submit} noValidate className="max-w-2xl space-y-4 rounded-lg border border-line bg-white p-6">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField
            label="Username"
            autoComplete="off"
            hint="Lowercase letters, digits, dot, underscore or hyphen."
            error={errors.username?.message}
            {...register('username', USERNAME_RULES)}
          />
          <TextField
            label="Email"
            type="email"
            autoComplete="off"
            hint="The one-time password is sent here."
            error={errors.email?.message}
            {...register('email', EMAIL_RULES)}
          />
        </div>
        <AccountRoleFields register={register} control={control} errors={errors} />
        <div className="flex gap-2 pt-2">
          <Button type="submit" loading={isSubmitting}>
            Create account
          </Button>
          <Button variant="secondary" onClick={() => navigate('/admin/users')} disabled={isSubmitting}>
            Cancel
          </Button>
        </div>
      </form>
    </>
  )
}
