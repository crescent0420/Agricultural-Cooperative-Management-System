import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { PageSpinner } from '../components/ui/Spinner'

/**
 * Guards every logged-in page (URD SEC-01). In order:
 *  1. no session            -> /login, and back here afterwards, unless the user logged out: the next
 *                               person to log in may be someone else, so they start on their own Dashboard
 *  2. one-time password      -> /change-password (FR-AUTH-02)
 *  3. farmer not linked yet  -> /not-linked (FR-AUTH-07)
 * The backend still checks every request; this only decides which screen to show.
 */
export function RequireAuth() {
  const { status, user, notice } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <PageSpinner label="Checking your session" />
  if (status === 'anonymous' || !user) {
    return <Navigate to="/login" replace state={notice === 'logged-out' ? null : { from: location }} />
  }

  if (user.mustChangePassword && location.pathname !== '/change-password') {
    return <Navigate to="/change-password" replace />
  }
  const unlinkedFarmer = user.role === 'FARMER' && user.farmerId === null
  if (!user.mustChangePassword && unlinkedFarmer && location.pathname !== '/not-linked') {
    return <Navigate to="/not-linked" replace />
  }
  return <Outlet />
}
