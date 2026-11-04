import { createContext } from 'react'
import type { CurrentUser, Permission } from '../types/auth'

/** Why the user is on the login page, shown there as a banner. */
export type LoginNotice = 'session-expired' | 'password-changed' | null

export interface AuthState {
  /** 'loading' only while a stored session is being checked at start-up. */
  status: 'loading' | 'anonymous' | 'authenticated'
  user: CurrentUser | null
  notice: LoginNotice
  /** Logs in and loads the current user. Throws ApiError on failure. */
  login: (username: string, password: string) => Promise<CurrentUser>
  logout: () => void
  /**
   * Changes the password, then logs in again with the new one, because the backend
   * invalidates the old token. Throws ApiError if the change itself is refused.
   */
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>
  hasPermission: (permission: Permission) => boolean
}

export const AuthContext = createContext<AuthState | null>(null)
