import { apiGet } from './client'

// Input contributions. This module only reads the farmer's own; the payments module adds the rest.

/** The logged-in farmer's contributions, one per season. sortBy: createdAt (and others, see the backend). */
export function listMyContributions(params, seasonId) {
  return apiGet('/me/contributions', { ...params, ...(seasonId ? { seasonId } : {}) })
}
