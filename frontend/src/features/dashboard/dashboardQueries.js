import { useQueries } from '@tanstack/react-query'
import { listOfferingResults } from '../../api/results'

/** At most this many offerings are checked for waiting results (decision D1). */
export const OFFERING_LIMIT = 100

/** The newest offerings first, as many as one page may hold. */
export const NEWEST_OFFERINGS = { page: 0, size: OFFERING_LIMIT, sortBy: 'createdAt', direction: 'desc' }

/** Seasons, newest first (one page holds them all in practice). */
export const ALL_SEASONS = { page: 0, size: 100, sortBy: 'startsOn', direction: 'desc' }

/**
 * Decision D2: the season whose enrolment window is open, otherwise the most recent one.
 * @param {{ enrolmentOpen: boolean }[]} seasons newest first
 */
export function currentSeason(seasons) {
  return seasons.find((s) => s.enrolmentOpen) ?? seasons[0] ?? null
}

/**
 * How many results of each offering have this status (DRAFT or SUBMITTED). The backend cannot count across
 * offerings (G9), so this asks each offering for a one-row page and reads totalElements (decision D1).
 * @returns {{ isPending: boolean, error: Error | null, data: { offering: object, count: number }[] | undefined }}
 */
export function useResultCounts(offerings, status) {
  const queries = useQueries({
    queries: (offerings ?? []).map((o) => ({
      queryKey: ['results', 'offering', o.id, 'count', status],
      queryFn: () => listOfferingResults(o.id, { page: 0, size: 1, sortBy: 'id', direction: 'asc' }, status),
    })),
  })
  if (!offerings) return { isPending: true, error: null, data: undefined }
  const error = queries.find((q) => q.error)?.error ?? null
  const isPending = !error && queries.some((q) => q.isPending)
  const data =
    isPending || error ? undefined : offerings.map((o, i) => ({ offering: o, count: queries[i].data.totalElements }))
  return { isPending, error, data }
}
