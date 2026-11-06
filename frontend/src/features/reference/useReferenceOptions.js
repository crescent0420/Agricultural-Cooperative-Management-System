import { useQuery } from '@tanstack/react-query'
import { ALL, listCooperatives, listDistricts, listProgrammes, listProvinces, listTrainings } from '../../api/reference'

// Dropdown lists, loaded once per session (they rarely change; saving a change refreshes them).

export function useDistrictOptions() {
  return useQuery({ queryKey: ['districts', 'options'], queryFn: () => listDistricts(ALL), staleTime: 5 * 60_000 })
}

export function useProvinceOptions() {
  return useQuery({ queryKey: ['provinces', 'options'], queryFn: () => listProvinces(ALL), staleTime: 5 * 60_000 })
}

export function useTrainingOptions() {
  return useQuery({ queryKey: ['trainings', 'options'], queryFn: () => listTrainings(ALL), staleTime: 5 * 60_000 })
}

/** The cooperatives of one district (none are loaded until a district is chosen). */
export function useCooperativeOptions(districtId) {
  return useQuery({
    queryKey: ['cooperatives', 'options', districtId],
    queryFn: () => listCooperatives(ALL, districtId),
    enabled: districtId != null,
    staleTime: 5 * 60_000,
  })
}

/** The crop programmes of one district. */
export function useProgrammeOptions(districtId) {
  return useQuery({
    queryKey: ['programmes', 'options', districtId],
    queryFn: () => listProgrammes(ALL, districtId),
    enabled: districtId != null,
    staleTime: 5 * 60_000,
  })
}
