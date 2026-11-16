import { apiGet, apiGetForMessage, apiPost, apiPostForMessage } from './client'

/** POST /api/auth/login (public). 401 wrong credentials, 422 email not verified. */
export function login(username, password) {
  return apiPost('/auth/login', { username, password })
}

/** GET /api/auth/me: who the token belongs to, with role, district, farmer link and permissions. */
export function getCurrentUser() {
  return apiGet('/auth/me')
}

/** POST /api/auth/change-password: 204. The current token stops working afterwards. */
export function changePassword(currentPassword, newPassword) {
  return apiPost('/auth/change-password', { currentPassword, newPassword })
}

/** POST /api/auth/register (public): returns the backend's confirmation message. */
export function register(username, email) {
  return apiPostForMessage('/auth/register', { username, email })
}

/**
 * GET /api/auth/verify-email (public): confirms the email address with the token from the email link. Works once;
 * returns the backend's confirmation message, or fails with 400 for an invalid, used or expired link.
 */
export function verifyEmail(token) {
  return apiGetForMessage('/auth/verify-email', { token })
}
