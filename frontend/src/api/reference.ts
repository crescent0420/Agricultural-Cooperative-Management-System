import type { District } from '../types/reference'
import { apiGet } from './client'

/** GET /api/districts/{id}: needs REFERENCE_READ, which every role holds. */
export function getDistrict(id: number): Promise<District> {
  return apiGet<District>(`/districts/${id}`)
}
