import type { ReactNode } from 'react'
import { useAuth } from '../auth/useAuth'
import { ForbiddenPage } from '../features/errors/ForbiddenPage'
import { canAccess, type Requirement } from '../navigation/access'

/**
 * Shows the page only to users holding the required permissions; others see "Not available to you".
 * A convenience for the user: the backend still refuses the API calls with 403 (URD SEC-03).
 */
export function RequirePermission({ requires, children }: { requires: Requirement; children: ReactNode }) {
  const { hasPermission } = useAuth()
  return canAccess(requires, hasPermission) ? children : <ForbiddenPage />
}
