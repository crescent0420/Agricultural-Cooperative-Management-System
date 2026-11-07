import { apiDelete, apiGet, apiPost, apiPut } from './client'

// An offering's inspections (URD FR-INS-01, 02). Only the offering's assigned Field Officer manages them.
// Weights are percentages: never above 100 in total (422); exactly 100 is needed before results are submitted.
// The list is frozen once a result of the offering has been submitted (409); an inspection that already
// has scores cannot change (409); a duplicate name answers 409.

/** @returns {Promise<import('../types/result').InspectionList>} not paged */
export function listInspections(offeringId) {
  return apiGet(`/offerings/${offeringId}/inspections`)
}

/** @param {{ name: string, maxScore: number, weight: number }} inspection */
export function createInspection(offeringId, inspection) {
  return apiPost(`/offerings/${offeringId}/inspections`, inspection)
}

export function updateInspection(id, inspection) {
  return apiPut(`/inspections/${id}`, inspection)
}

export function deleteInspection(id) {
  return apiDelete(`/inspections/${id}`)
}
