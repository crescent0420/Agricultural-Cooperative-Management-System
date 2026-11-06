import { createContext } from 'react'

/**
 * Why the user is on the login page, shown there as a banner.
 * @typedef {'session-expired' | 'password-changed' | null} LoginNotice
 */

/**
 * What useAuth() returns.
 * @typedef {object} AuthState
 * @property {'loading' | 'anonymous' | 'authenticated'} status  'loading' only while a stored session is checked at start-up
 * @property {import('../types/auth').CurrentUser | null} user
 * @property {LoginNotice} notice
 * @property {(username: string, password: string) => Promise<import('../types/auth').CurrentUser>} login
 *   Logs in and loads the current user. Throws ApiError on failure.
 * @property {() => void} logout
 * @property {(currentPassword: string, newPassword: string) => Promise<void>} changePassword
 *   Changes the password, then logs in again with the new one, because the backend invalidates
 *   the old token. Throws ApiError if the change itself is refused.
 * @property {(permission: import('../types/auth').Permission) => boolean} hasPermission
 */

/** @type {import('react').Context<AuthState | null>} */
export const AuthContext = createContext(null)
