import { apiGet, apiPatch, apiPost, apiPostForMessage } from './client'

// User accounts (URD FR-ADM-01..05). Every call needs USER_MANAGE (System Administrator).
// The backend reloads the account from the database on every request, so a role change or a disabled
// account takes effect from that user's next request.

/** FR-ADM-01: sortBy id, username, role, createdAt. No filters (gap G10). */
export function listUsers(params) {
  return apiGet('/users', { ...params })
}

export function getUser(id) {
  return apiGet(`/users/${id}`)
}

/**
 * FR-ADM-02: 201 with the new account; the backend generates a one-time password and emails it.
 * 400 district/farmer does not fit the role, 409 username or email in use / farmer already has an account.
 * @param {{ username: string, email: string, role: string, districtId: number | null, farmerId: number | null }} account
 */
export function createUser(account) {
  return apiPost('/users', account)
}

/** FR-ADM-03: also links an unlinked farmer account. Own role → 409. */
export function changeUserRole(id, { role, districtId, farmerId }) {
  return apiPatch(`/users/${id}/role`, { role, districtId, farmerId })
}

/** FR-ADM-04: disabling one's own account → 409. */
export function setUserEnabled(id, enabled) {
  return apiPatch(`/users/${id}/enabled`, { enabled })
}

/** FR-ADM-05: emails a new one-time password. 422 no email address / account disabled. Returns the message. */
export function resetUserPassword(id) {
  return apiPostForMessage(`/users/${id}/reset-password`)
}
