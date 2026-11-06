import { useQuery } from '@tanstack/react-query'
import { listSeasons } from '../../api/seasons'

/** Every season, newest first, for dropdowns (the API allows 100 per page; the Union has a few per year). */
export const ALL_SEASONS = { page: 0, size: 100, sortBy: 'startsOn', direction: 'desc' }

export function useSeasonOptions() {
  return useQuery({ queryKey: ['seasons', 'options'], queryFn: () => listSeasons(ALL_SEASONS), staleTime: 5 * 60_000 })
}
