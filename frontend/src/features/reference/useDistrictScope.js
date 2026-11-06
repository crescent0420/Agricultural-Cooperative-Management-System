import { useAuth } from '../../auth/useAuth'

/** The district to send: the user's own for district staff, otherwise the chosen filter (or none). */
export function useDistrictScope(filterValue) {
  const { user } = useAuth()
  if (user?.districtId != null) return user.districtId
  return filterValue ? Number(filterValue) : undefined
}
