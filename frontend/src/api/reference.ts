import type { PagedResponse, PageParams } from '../types/api'
import type { Cooperative, CropProgramme, District, GradingBand, Province, Training } from '../types/reference'
import { apiDelete, apiGet, apiPost, apiPut } from './client'

// Reference data (URD FR-REF-01..07). Reads need REFERENCE_READ (every role);
// writes need REFERENCE_MANAGE or GRADING_SCALE_MANAGE (Union Registrar).

/** Large enough for every dropdown list (30 districts, a handful of provinces and trainings); the API allows 100. */
export const ALL: PageParams = { page: 0, size: 100, sortBy: 'name', direction: 'asc' }

export function listProvinces(params: PageParams = ALL): Promise<PagedResponse<Province>> {
  return apiGet('/provinces', { ...params })
}

export function listDistricts(params: PageParams, provinceId?: number): Promise<PagedResponse<District>> {
  return apiGet('/districts', { ...params, provinceId })
}

export function getDistrict(id: number): Promise<District> {
  return apiGet<District>(`/districts/${id}`)
}

export function renameDistrict(id: number, name: string): Promise<District> {
  return apiPut(`/districts/${id}`, { name })
}

/** District staff (Field Officers, Agronomists) are limited to their own district by the backend. */
export function listCooperatives(params: PageParams, districtId?: number): Promise<PagedResponse<Cooperative>> {
  return apiGet('/cooperatives', { ...params, districtId })
}

export function createCooperative(name: string, districtId: number): Promise<Cooperative> {
  return apiPost('/cooperatives', { name, districtId })
}

export function renameCooperative(id: number, name: string): Promise<Cooperative> {
  return apiPut(`/cooperatives/${id}`, { name })
}

export function listProgrammes(params: PageParams, districtId?: number): Promise<PagedResponse<CropProgramme>> {
  return apiGet('/programmes', { ...params, districtId })
}

export function getProgramme(id: number): Promise<CropProgramme> {
  return apiGet(`/programmes/${id}`)
}

export function createProgramme(name: string, districtId: number): Promise<CropProgramme> {
  return apiPost('/programmes', { name, districtId })
}

export function renameProgramme(id: number, name: string): Promise<CropProgramme> {
  return apiPut(`/programmes/${id}`, { name })
}

export function addPrerequisite(programmeId: number, trainingId: number): Promise<CropProgramme> {
  return apiPost(`/programmes/${programmeId}/prerequisites`, { trainingId })
}

export function removePrerequisite(programmeId: number, trainingId: number): Promise<CropProgramme> {
  return apiDelete(`/programmes/${programmeId}/prerequisites/${trainingId}`)
}

export function listTrainings(params: PageParams = ALL): Promise<PagedResponse<Training>> {
  return apiGet('/trainings', { ...params })
}

export function createTraining(name: string): Promise<Training> {
  return apiPost('/trainings', { name })
}

export function renameTraining(id: number, name: string): Promise<Training> {
  return apiPut(`/trainings/${id}`, { name })
}

export function getGradingScale(): Promise<GradingBand[]> {
  return apiGet('/grading-scale')
}

/** Replaces the whole scale: up to 10 bands, one of them starting at 0. */
export function replaceGradingScale(bands: GradingBand[]): Promise<GradingBand[]> {
  return apiPut('/grading-scale', { bands })
}
