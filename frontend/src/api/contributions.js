import { apiGet, apiPost } from './client'

// Input contributions (URD FR-FIN-01..04, FR-FIN-07). The backend computes every figure: confirmed paid,
// pending, outstanding, % paid, and the two flags (enrolmentAllowed at 30%, inputCollectionAllowed at 100%).
// The browser shows them as they come and calculates nothing (FR-FIN-01).

const clean = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))

/** FR-FIN-01: sortBy id, createdAt, amountDue. @param {{ farmerId?: number, seasonId?: number }} filters */
export function listContributions(params, filters = {}) {
  return apiGet('/contributions', { ...params, ...clean(filters) })
}

/** FR-FIN-02: one obligation per farmer and season (a second answers 409). */
export function createContribution({ farmerId, seasonId, amountDue }) {
  return apiPost('/contributions', { farmerId, seasonId, amountDue })
}

/** FR-FIN-04 */
export function getContribution(id) {
  return apiGet(`/contributions/${id}`)
}

/** FR-FIN-03: same amount → 400; below what is already confirmed → 422. */
export function adjustContribution(id, { newAmountDue, reason }) {
  return apiPost(`/contributions/${id}/adjustments`, { newAmountDue, reason })
}

/** FR-FIN-03: sortBy id, adjustedAt. Registrar and Finance only. */
export function listAdjustments(id, params) {
  return apiGet(`/contributions/${id}/adjustments`, { ...params })
}

/** FR-FIN-04: sortBy id, requestedAt, amount, status. */
export function listContributionPayments(id, params) {
  return apiGet(`/contributions/${id}/payments`, { ...params })
}

/** FR-FIN-07: the logged-in farmer's contributions, one per season. */
export function listMyContributions(params, seasonId) {
  return apiGet('/me/contributions', { ...params, ...(seasonId ? { seasonId } : {}) })
}
