import { apiGet, apiPost } from './client'

// Enrolment (URD FR-ENR-01..06). The backend checks, in this order: district, window open, farmer Active,
// at least 30% of the season's contribution paid, prerequisites, not already enrolled, the season's land
// limit, a place left. A refusal answers 422 (a rule) or 409 (already enrolled, full) with the reason.

const clean = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))

/** FR-ENR-02: the logged-in farmer enrols; the hectares come from the offering, so there is no body. */
export function enrol(offeringId) {
  return apiPost(`/offerings/${offeringId}/enrolments`)
}

/**
 * FR-ENR-05: the farmers enrolled in an offering. status is ENROLLED (the backend's default) or CANCELLED.
 * sortBy: id, enrolledAt, status.
 */
export function listOfferingEnrolments(offeringId, params, status) {
  return apiGet(`/offerings/${offeringId}/enrolments`, { ...params, ...clean({ status }) })
}

/** FR-ENR-03: the logged-in farmer's enrolments. @param {{ seasonId?: number, status?: string }} filters */
export function listMyEnrolments(params, filters = {}) {
  return apiGet('/me/enrolments', { ...params, ...clean(filters) })
}

/** FR-ENR-06 */
export function getEnrolment(id) {
  return apiGet(`/enrolments/${id}`)
}

/**
 * FR-ENR-04. reason (max 500) is optional for the farmer and required for the Registrar (400 otherwise).
 * A farmer may cancel only while the window is open (422); nobody after the season has ended (422).
 */
export function cancelEnrolment(id, reason) {
  return apiPost(`/enrolments/${id}/cancel`, { reason: reason || null })
}
