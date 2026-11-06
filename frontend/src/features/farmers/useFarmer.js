import { useOutletContext } from 'react-router'

/** The farmer loaded by FarmerDetailPage, for its tabs. @returns {import('../../types/farmer').Farmer} */
export function useFarmer() {
  return useOutletContext()
}
