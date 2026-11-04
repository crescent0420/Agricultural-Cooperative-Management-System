import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { register as registerAccount } from '../../api/auth'
import { ApiError, toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { TextField } from '../../components/ui/TextField'
import { AuthLayout } from './AuthLayout'

interface RegisterForm {
  username: string
  email: string
}

/** FR-AUTH-04: self-registration. The account becomes usable after email verification. */
export function RegisterPage() {
  const [failure, setFailure] = useState<ApiError | null>(null)
  const [confirmation, setConfirmation] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({ defaultValues: { username: '', email: '' } })

  const onSubmit = handleSubmit(async ({ username, email }) => {
    setFailure(null)
    try {
      setConfirmation(await registerAccount(username.trim(), email.trim()))
    } catch (error) {
      const apiError = toApiError(error)
      for (const issue of apiError.fieldErrors) {
        if (issue.field === 'username' || issue.field === 'email') {
          setError(issue.field, { message: issue.message })
        }
      }
      setFailure(apiError)
    }
  })

  if (confirmation) {
    return (
      <AuthLayout title="Check your email">
        <div className="space-y-4">
          <Alert tone="success">{confirmation}</Alert>
          <p className="text-sm text-muted">
            Open the verification link in the email, then log in with the one-time password it contains.
          </p>
          <Link to="/login" className="text-sm font-semibold text-brand-600 hover:underline">
            Go to log in
          </Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Create an account" intro="For farmers of the Rwanda Cooperative Union.">
      <div className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <form onSubmit={onSubmit} noValidate className="space-y-4">
          <TextField
            label="Username"
            autoComplete="username"
            hint="3 to 50 characters: lowercase letters, digits, dot, underscore or hyphen."
            error={errors.username?.message}
            {...register('username', {
              required: 'Choose a username.',
              pattern: {
                value: /^[a-z0-9._-]{3,50}$/,
                message: 'Use 3 to 50 lowercase letters, digits, dots, underscores or hyphens.',
              },
            })}
          />
          <TextField
            label="Email address"
            type="email"
            autoComplete="email"
            error={errors.email?.message}
            {...register('email', {
              required: 'Enter your email address.',
              maxLength: { value: 254, message: 'This email address is too long.' },
              pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address.' },
            })}
          />
          <Button type="submit" fullWidth loading={isSubmitting}>
            Create account
          </Button>
        </form>
        <p className="text-sm text-muted">
          Already have an account?{' '}
          <Link to="/login" className="font-semibold text-brand-600 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </AuthLayout>
  )
}
