import { apiGet, apiPost, apiPut } from './client'

// Results: Draft → Submitted → Published (URD FR-RES-01..07).
// The assigned Field Officer enters scores and submits; the District Agronomist of the programme's district
// approves (publishes) or returns, and corrects published results. A farmer reads only their own published ones.

const clean = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))

/**
 * FR-RES-01: create or change the DRAFT result of an enrolment. Only the scores sent change.
 * @param {{ inspectionId: number, score: number }[]} scores  each between 0 and the inspection's maximum
 */
export function enterScores(enrolmentId, scores) {
  return apiPut(`/enrolments/${enrolmentId}/scores`, { scores })
}

/** FR-RES-02: 422 if a score is missing or the weights do not add up to 100%. */
export function submitResult(id) {
  return apiPost(`/results/${id}/submit`)
}

/** FR-RES-04: publishes the result to the farmer. */
export function approveResult(id) {
  return apiPost(`/results/${id}/approve`)
}

/** FR-RES-04: back to the Field Officer as a DRAFT; the reason (max 500) is required. */
export function returnResult(id, reason) {
  return apiPost(`/results/${id}/return`, { reason })
}

/**
 * FR-RES-05: correct one score of a PUBLISHED result. The reason (max 500) is required; the same score
 * answers 400. The original score is kept and the correction joins the history.
 */
export function correctResult(id, { inspectionId, correctedScore, reason }) {
  return apiPost(`/results/${id}/corrections`, { inspectionId, correctedScore, reason })
}

/** FR-RES-06 */
export function getResult(id) {
  return apiGet(`/results/${id}`)
}

/** FR-RES-06: oldest first. Not paged. */
export function listCorrections(id) {
  return apiGet(`/results/${id}/corrections`)
}

/** FR-RES-03: sortBy id, createdAt, finalScore, status; status DRAFT, SUBMITTED or PUBLISHED (none = all). */
export function listOfferingResults(offeringId, params, status) {
  return apiGet(`/offerings/${offeringId}/results`, { ...params, ...clean({ status }) })
}

/** FR-RES-07: the farmer's own PUBLISHED results. */
export function listMyResults(params) {
  return apiGet('/me/results', { ...params })
}
