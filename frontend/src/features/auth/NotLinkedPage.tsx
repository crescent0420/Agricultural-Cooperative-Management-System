import { Navigate } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/ui/Button'
import { AuthLayout } from './AuthLayout'

/** FR-AUTH-07 (URD gap G4): a farmer account not yet linked to a farmer record. */
export function NotLinkedPage() {
  const { user, logout } = useAuth()
  if (user && !(user.role === 'FARMER' && user.farmerId === null)) return <Navigate to="/" replace />

  return (
    <AuthLayout title="Your account is not linked yet">
      <div className="space-y-4 text-ink">
        <p>
          Your account <strong>{user?.username}</strong> is not yet linked to a farmer record, so there is nothing to
          show yet.
        </p>
        <p className="text-muted">
          Please contact the system administrator of the Rwanda Cooperative Union and ask them to link your account to
          your farmer registration.
        </p>
        <Button variant="secondary" onClick={logout}>
          Log out
        </Button>
      </div>
    </AuthLayout>
  )
}
