import { useOutletContext } from 'react-router'

/** The offering loaded by OfferingPage, for its tabs. @returns {import('../../types/season').Offering} */
export function useOffering() {
  return useOutletContext()
}
