import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { ApiError, toApiError } from '../../api/errors'
import { useAuth } from '../../auth/useAuth'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { PasswordField } from '../../components/ui/PasswordField'
import { AuthLayout } from './AuthLayout'

interface ChangePasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * FR-AUTH-02 (forced, after a one-time password) and FR-AUTH-03 (voluntary).
 * The backend rules: new password 12-128 characters and different from the current one.
 */
export function ChangePasswordPage() {
  const { user, changePassword, logout } = useAuth()
  const navigate = useNavigate()
  const forced = user?.mustChangePassword ?? false
  const [failure, setFailure] = useState<ApiError | null>(null)
  const {
    register,
    handleSubmit,
    getValues,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordForm>({ defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' } })

  const onSubmit = handleSubmit(async ({ currentPassword, newPassword }) => {
    setFailure(null)
    try {
      await changePassword(currentPassword, newPassword)
      navigate('/', { replace: true, state: { notice: 'Your password has been changed.' } })
    } catch (error) {
      const apiError = toApiError(error)
      for (const issue of apiError.fieldErrors) {
        if (issue.field === 'currentPassword' || issue.field === 'newPassword') {
          setError(issue.field, { message: issue.message })
        }
      }
      setFailure(apiError)
    }
  })

  return (
    <AuthLayout
      title={forced ? 'Choose your own password' : 'Change password'}
      intro={
        forced
          ? 'You logged in with a one-time password. Choose your own password to continue.'
          : 'Enter your current password and the new one.'
      }
    >
      <div className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          <PasswordField
            label={forced ? 'One-time password' : 'Current password'}
            autoComplete="current-password"
            error={errors.currentPassword?.message}
            {...register('currentPassword', { required: 'Enter your current password.' })}
          />
          <PasswordField
            label="New password"
            autoComplete="new-password"
            hint="12 to 128 characters, different from the current password."
            error={errors.newPassword?.message}
            {...register('newPassword', {
              required: 'Enter a new password.',
              minLength: { value: 12, message: 'Use at least 12 characters.' },
              maxLength: { value: 128, message: 'Use at most 128 characters.' },
              validate: (value) =>
                value !== getValues('currentPassword') || 'The new password must differ from the current one.',
            })}
          />
          <PasswordField
            label="Confirm new password"
            autoComplete="new-password"
            error={errors.confirmPassword?.message}
            {...register('confirmPassword', {
              required: 'Type the new password again.',
              validate: (value) => value === getValues('newPassword') || 'The two new passwords do not match.',
            })}
          />
          <Button type="submit" fullWidth loading={isSubmitting}>
            Save new password
          </Button>
        </form>
        {forced ? (
          <Button variant="link" onClick={logout}>
            Log out instead
          </Button>
        ) : (
          <Link to="/" className="text-sm font-semibold text-brand-600 hover:underline">
            Cancel
          </Link>
        )}
      </div>
    </AuthLayout>
  )
}
