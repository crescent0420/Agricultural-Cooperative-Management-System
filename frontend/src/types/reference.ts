// TypeScript copies of the backend's reference-data DTOs.

/** com.acms.dto.ProvinceResponse */
export interface Province {
  id: number
  name: string
}

/** com.acms.dto.DistrictResponse */
export interface District {
  id: number
  name: string
  provinceId: number | null
  provinceName: string | null
}

/** com.acms.dto.CooperativeResponse */
export interface Cooperative {
  id: number
  name: string
  districtId: number
  districtName: string
}

/** com.acms.dto.TrainingResponse */
export interface Training {
  id: number
  name: string
}

/** com.acms.dto.CropProgrammeResponse */
export interface CropProgramme {
  id: number
  name: string
  districtId: number
  districtName: string
  prerequisites: Training[]
}

/** com.acms.dto.GradingBandDto */
export interface GradingBand {
  grade: string
  minScore: number
  description: string | null
}
