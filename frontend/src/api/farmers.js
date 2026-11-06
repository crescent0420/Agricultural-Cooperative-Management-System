import { apiGet, apiPatch, apiPost, apiPut } from './client'

// Farmers and their training records (URD FR-FAR-01..10).
// The backend decides who may do what: FARMER_LIST, FARMER_READ, FARMER_MANAGE, FARMER_STATUS_CHANGE,
// TRAINING_RECORD_READ/MANAGE and RESULT_READ, and limits Field Officers and Agronomists to their district.

/**
 * FR-FAR-01: search, paged and sorted (sortBy: id, fullName, membershipNumber, status, createdAt).
 * Empty filters are left out of the address.
 * @param {{ page: number, size: number, sortBy: string, direction: string }} params
 * @param {{ name?: string, membershipNumber?: string, cooperativeId?: number, status?: string }} filters
 */
export function searchFarmers(params, filters = {}) {
  const clean = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))
  return apiGet('/farmers', { ...params, ...clean })
}

/** FR-FAR-02 */
export function getFarmer(id) {
  return apiGet(`/farmers/${id}`)
}

/**
 * FR-FAR-03. The backend generates the membership number; a national ID already registered answers 409.
 * @param {{ fullName: string, nationalId: string, phoneNumber: string | null, cooperativeId: number }} farmer
 */
export function registerFarmer(farmer) {
  return apiPost('/farmers', farmer)
}

/** FR-FAR-04: only the name and the phone number can change. */
export function updateFarmer(id, { fullName, phoneNumber }) {
  return apiPut(`/farmers/${id}`, { fullName, phoneNumber })
}

/** FR-FAR-05: a change the backend does not allow answers 422 with the reason. */
export function changeFarmerStatus(id, status) {
  return apiPatch(`/farmers/${id}/status`, { status })
}

/** FR-FAR-06: sortBy id, completedOn or recordedAt. */
export function listFarmerTrainings(farmerId, params) {
  return apiGet(`/farmers/${farmerId}/trainings`, { ...params })
}

/**
 * FR-FAR-07: completedOn is "YYYY-MM-DD", today or earlier; the same training twice answers 409.
 */
export function recordFarmerTraining(farmerId, trainingId, completedOn) {
  return apiPost(`/farmers/${farmerId}/trainings`, { trainingId, completedOn })
}

/** FR-FAR-08: the programme's prerequisite trainings this farmer has not completed (an empty list = none). */
export function getMissingPrerequisites(farmerId, programmeId) {
  return apiGet(`/farmers/${farmerId}/missing-prerequisites`, { programmeId })
}

/** FR-FAR-09: sortBy id, createdAt, finalScore or status. The backend chooses which results each role sees. */
export function listFarmerResults(farmerId, params) {
  return apiGet(`/farmers/${farmerId}/results`, { ...params })
}

/** FR-FAR-10: the logged-in farmer's own record. */
export function getMyProfile() {
  return apiGet('/me/profile')
}
