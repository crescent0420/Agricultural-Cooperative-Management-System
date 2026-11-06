import { useQuery } from '@tanstack/react-query'
import { listFieldOfficers } from '../../api/offerings'

/** G8: the enabled Field Officers of one district, for the officer dropdown (nothing loads without a district). */
export function useFieldOfficers(districtId) {
  return useQuery({
    queryKey: ['field-officers', districtId],
    queryFn: () => listFieldOfficers(districtId),
    enabled: districtId != null,
  })
}
