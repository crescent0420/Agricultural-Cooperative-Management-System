import { useQuery } from '@tanstack/react-query'
import { ALL, listDistricts, listProvinces, listTrainings } from '../../api/reference'

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
