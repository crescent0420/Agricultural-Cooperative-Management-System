import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { toApiError } from '../../api/errors'
import { useAuth } from '../../auth/useAuth'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { PasswordField } from '../../components/ui/PasswordField'
import { TextField } from '../../components/ui/TextField'
import { AuthLayout } from './AuthLayout'

/** Where to go after login: the page the user was sent away from, or home. */
function useReturnPath() {
  const location = useLocation()
  const from = location.state?.from
  return from ? `${from.pathname}${from.search ?? ''}` : '/'
}

/** FR-AUTH-01: log in with username and password. */
export function LoginPage() {
  const { status, notice, login } = useAuth()
  const navigate = useNavigate()
  const returnPath = useReturnPath()
  const [failure, setFailure] = useState(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { username: '', password: '' } })

  if (status === 'authenticated') return <Navigate to={returnPath} replace />

  const onSubmit = handleSubmit(async ({ username, password }) => {
    setFailure(null)
    try {
      await login(username.trim(), password)
      navigate(returnPath, { replace: true })
    } catch (error) {
      const apiError = toApiError(error)
      for (const issue of apiError.fieldErrors) {
        if (issue.field === 'username' || issue.field === 'password') {
          setError(issue.field, { message: issue.message })
        }
      }
      setFailure(apiError)
    }
  })

  return (
    <AuthLayout title="Log in" intro="Use the account given to you by the system administrator.">
      <div className="space-y-4">
        {notice === 'session-expired' && !failure && (
          <Alert tone="warning">Your session has expired. Please log in again.</Alert>
        )}
        {notice === 'password-changed' && !failure && (
          <Alert tone="success">Your password was changed. Please log in with your new password.</Alert>
        )}
        {failure && <Alert tone="error">{failure.message}</Alert>}

        <form onSubmit={onSubmit} noValidate className="space-y-4">
          <TextField
            label="Username"
            autoComplete="username"
            autoFocus
            error={errors.username?.message}
            {...register('username', {
              required: 'Enter your username.',
              maxLength: { value: 100, message: 'A username has at most 100 characters.' },
            })}
          />
          <PasswordField
            label="Password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password', {
              required: 'Enter your password.',
              maxLength: { value: 72, message: 'A password has at most 72 characters.' },
            })}
          />
          <Button type="submit" fullWidth loading={isSubmitting}>
            Log in
          </Button>
        </form>

        <p className="text-sm text-muted">
          Farmer without an account?{' '}
          <Link to="/register" className="font-semibold text-brand-600 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </AuthLayout>
  )
}
