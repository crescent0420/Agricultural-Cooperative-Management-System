import { apiGet, apiPost, apiPut } from './client'

// Seasons and enrolment windows (URD FR-SEA-01..04). Reading needs SEASON_READ (every role);
// creating, editing and opening or closing windows need SEASON_MANAGE (Union Registrar).

/** sortBy: id, name, startsOn, endsOn, createdAt. */
export function listSeasons(params) {
  return apiGet('/seasons', { ...params })
}

export function getSeason(id) {
  return apiGet(`/seasons/${id}`)
}

/**
 * @param {{ name: string, startsOn: string, endsOn: string, maxLandHectares: number, distributionDate: string }} season
 *   Dates as "YYYY-MM-DD". A duplicate name answers 409.
 */
export function createSeason(season) {
  return apiPost('/seasons', season)
}

/** Refused with 409 once an enrolment window has ever been opened for the season. */
export function updateSeason(id, season) {
  return apiPut(`/seasons/${id}`, season)
}

/** 409 if a window is already open; 422 if the season has ended. */
export function openWindow(seasonId) {
  return apiPost(`/seasons/${seasonId}/windows/open`)
}

/** 409 if no window is open. */
export function closeWindow(seasonId) {
  return apiPost(`/seasons/${seasonId}/windows/close`)
}

/** sortBy: id, openedAt, closedAt. */
export function listWindows(seasonId, params) {
  return apiGet(`/seasons/${seasonId}/windows`, { ...params })
}
