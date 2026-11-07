import { apiClient, apiGet } from './client'

// Collection permits (URD FR-PER-01..04). The backend issues a permit only for the farmer's own, valid
// enrolment, while the farmer is Active, before the distribution day and with the contribution 100% paid;
// it records every attempt, issued or refused.

const clean = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))

/**
 * FR-PER-01: 201 = a new permit, 200 = the permit this enrolment already has (the same one), 422 = refused
 * with the reason.
 * @returns {Promise<{ permit: object, created: boolean }>}
 */
export async function generatePermit(enrolmentId) {
  const response = await apiClient.post(`/enrolments/${enrolmentId}/permit`)
  return { permit: response.data.data, created: response.status === 201 }
}

/** FR-PER-02: sortBy id, issuedAt. */
export function listMyPermits(params) {
  return apiGet('/me/permits', { ...params })
}

/** FR-PER-03: checked against today's data. Unknown number → 404. */
export function verifyPermit(permitNumber) {
  return apiGet(`/permits/verify/${encodeURIComponent(permitNumber)}`)
}

/**
 * FR-PER-04: sortBy id, attemptedAt.
 * @param {{ farmerId?: number, enrolmentId?: number, outcome?: 'ISSUED' | 'ALREADY_ISSUED' | 'REFUSED' }} filters
 */
export function listPermitAttempts(params, filters = {}) {
  return apiGet('/permits/attempts', { ...params, ...clean(filters) })
}
