import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { verifyEmail } from '../../api/auth'
import { Alert } from '../../components/ui/Alert'
import { Spinner } from '../../components/ui/Spinner'
import { AuthLayout } from './AuthLayout'

function LogInLink() {
  return (
    <Link to="/login" className="inline-block text-sm font-semibold text-brand-600 hover:underline">
      Go to log in
    </Link>
  )
}

/**
 * /verify-email?token=...: the page the verification link in an account email opens (FR-AUTH-01, FR-AUTH-04).
 * It confirms the address once through GET /api/auth/verify-email and says what to do next. The token works only
 * once, so the request is made exactly once: no retry, no refetch when the window regains focus.
 */
export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim() ?? ''
  const verification = useQuery({
    queryKey: ['verify-email', token],
    queryFn: () => verifyEmail(token),
    enabled: token !== '',
    retry: false,
    staleTime: Infinity,
    gcTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })

  if (token === '') {
    return (
      <AuthLayout title="Confirm your email address">
        <div className="space-y-4">
          <Alert tone="error">
            This link is incomplete. Open the link exactly as it appears in your account email.
          </Alert>
          <LogInLink />
        </div>
      </AuthLayout>
    )
  }

  if (verification.isPending) {
    return (
      <AuthLayout title="Confirm your email address">
        <div className="py-6 text-brand-700">
          <Spinner label="Confirming your email address" />
        </div>
      </AuthLayout>
    )
  }

  if (verification.error) {
    return (
      <AuthLayout title="Confirm your email address">
        <div className="space-y-4">
          <Alert tone="error">{verification.error.message}</Alert>
          <p className="text-sm text-muted">
            A link works once, for 24 hours. If you already used it, simply log in. Otherwise ask your system
            administrator to send you a new email (Reset password).
          </p>
          <LogInLink />
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Email address confirmed">
      <div className="space-y-4">
        <Alert tone="success">{verification.data}</Alert>
        <p className="text-sm text-muted">
          Log in with the one-time password from the same email. You will then choose your own password.
        </p>
        <LogInLink />
      </div>
    </AuthLayout>
  )
}
