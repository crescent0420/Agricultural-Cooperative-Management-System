import { apiGet, apiPost, apiPut } from './client'

// Programme offerings (URD FR-OFF-01..06). Reading needs OFFERING_READ; the backend limits Field Officers,
// Agronomists and farmers to their own district. Creating and editing need OFFERING_MANAGE (Union Registrar).

/** Without a value, a filter is left out of the address. */
const clean = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))

/**
 * sortBy: id, createdAt, maxParticipants, enrolledCount, hectaresPerParticipant.
 * @param {{ seasonId?: number, programmeId?: number, districtId?: number }} filters
 */
export function listOfferings(params, filters = {}) {
  return apiGet('/offerings', { ...params, ...clean(filters) })
}

/** FR-OFF-02: the logged-in Field Officer's own offerings. */
export function listMyOfferings(params, seasonId) {
  return apiGet('/offerings/assigned-to-me', { ...params, ...clean({ seasonId }) })
}

export function getOffering(id) {
  return apiGet(`/offerings/${id}`)
}

/**
 * @param {{ programmeId: number, seasonId: number, fieldOfficerId: number, hectaresPerParticipant: number,
 *           maxParticipants: number }} offering
 *   The same programme twice in one season answers 409; an ended season, a Field Officer of another district
 *   or more hectares than the season's land limit answer 422.
 */
export function createOffering(offering) {
  return apiPost('/offerings', offering)
}

/** The programme and season never change. 409 below the enrolled count, or new hectares after enrolments. */
export function updateOffering(id, { fieldOfficerId, hectaresPerParticipant, maxParticipants }) {
  return apiPut(`/offerings/${id}`, { fieldOfficerId, hectaresPerParticipant, maxParticipants })
}

/** G8 (FR-OFF-06): enabled Field Officers of a district, for the officer dropdown. Not paged. */
export function listFieldOfficers(districtId) {
  return apiGet('/field-officers', { ...clean({ districtId }) })
}
