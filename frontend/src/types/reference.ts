// TypeScript copies of the backend's reference-data DTOs.

/** GET /api/districts/{id} -> data (com.acms.dto.DistrictResponse) */
export interface District {
  id: number
  name: string
  provinceId: number | null
  provinceName: string | null
}
