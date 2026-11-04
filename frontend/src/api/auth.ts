import type { CurrentUser, LoginResponse } from '../types/auth'
import { apiGet, apiPost, apiPostForMessage } from './client'

/** POST /api/auth/login (public). 401 wrong credentials, 422 email not verified. */
export function login(username: string, password: string): Promise<LoginResponse> {
  return apiPost<LoginResponse>('/auth/login', { username, password })
}

/** GET /api/auth/me: who the token belongs to, with role, district, farmer link and permissions. */
export function getCurrentUser(): Promise<CurrentUser> {
  return apiGet<CurrentUser>('/auth/me')
}

/** POST /api/auth/change-password: 204. The current token stops working afterwards. */
export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiPost<void>('/auth/change-password', { currentPassword, newPassword })
}

/** POST /api/auth/register (public): returns the backend's confirmation message. */
export function register(username: string, email: string): Promise<string> {
  return apiPostForMessage('/auth/register', { username, email })
}
