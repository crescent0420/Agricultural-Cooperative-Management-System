import { apiDelete, apiGet, apiPost, apiPut } from './client'

// Reference data (URD FR-REF-01..07). Reads need REFERENCE_READ (every role);
// writes need REFERENCE_MANAGE or GRADING_SCALE_MANAGE (Union Registrar).

/** Large enough for every dropdown list (30 districts, a handful of provinces and trainings); the API allows 100. */
export const ALL = { page: 0, size: 100, sortBy: 'name', direction: 'asc' }

export function listProvinces(params = ALL) {
  return apiGet('/provinces', { ...params })
}

export function listDistricts(params, provinceId) {
  return apiGet('/districts', { ...params, provinceId })
}

export function getDistrict(id) {
  return apiGet(`/districts/${id}`)
}

export function renameDistrict(id, name) {
  return apiPut(`/districts/${id}`, { name })
}

/** District staff (Field Officers, Agronomists) are limited to their own district by the backend. */
export function listCooperatives(params, districtId) {
  return apiGet('/cooperatives', { ...params, districtId })
}

export function createCooperative(name, districtId) {
  return apiPost('/cooperatives', { name, districtId })
}

export function renameCooperative(id, name) {
  return apiPut(`/cooperatives/${id}`, { name })
}

export function listProgrammes(params, districtId) {
  return apiGet('/programmes', { ...params, districtId })
}

export function getProgramme(id) {
  return apiGet(`/programmes/${id}`)
}

export function createProgramme(name, districtId) {
  return apiPost('/programmes', { name, districtId })
}

export function renameProgramme(id, name) {
  return apiPut(`/programmes/${id}`, { name })
}

export function addPrerequisite(programmeId, trainingId) {
  return apiPost(`/programmes/${programmeId}/prerequisites`, { trainingId })
}

export function removePrerequisite(programmeId, trainingId) {
  return apiDelete(`/programmes/${programmeId}/prerequisites/${trainingId}`)
}

export function listTrainings(params = ALL) {
  return apiGet('/trainings', { ...params })
}

export function createTraining(name) {
  return apiPost('/trainings', { name })
}

export function renameTraining(id, name) {
  return apiPut(`/trainings/${id}`, { name })
}

export function getGradingScale() {
  return apiGet('/grading-scale')
}

/** Replaces the whole scale: up to 10 bands, one of them starting at 0. */
export function replaceGradingScale(bands) {
  return apiPut('/grading-scale', { bands })
}
