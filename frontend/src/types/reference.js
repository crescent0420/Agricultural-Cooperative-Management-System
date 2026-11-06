// The backend's reference-data DTOs, written as JSDoc so editors can show them. Documentation only.

/**
 * com.acms.dto.ProvinceResponse
 * @typedef {object} Province
 * @property {number} id
 * @property {string} name
 */

/**
 * com.acms.dto.DistrictResponse
 * @typedef {object} District
 * @property {number} id
 * @property {string} name
 * @property {number | null} provinceId
 * @property {string | null} provinceName
 */

/**
 * com.acms.dto.CooperativeResponse
 * @typedef {object} Cooperative
 * @property {number} id
 * @property {string} name
 * @property {number} districtId
 * @property {string} districtName
 */

/**
 * com.acms.dto.TrainingResponse
 * @typedef {object} Training
 * @property {number} id
 * @property {string} name
 */

/**
 * com.acms.dto.CropProgrammeResponse
 * @typedef {object} CropProgramme
 * @property {number} id
 * @property {string} name
 * @property {number} districtId
 * @property {string} districtName
 * @property {Training[]} prerequisites
 */

/**
 * com.acms.dto.GradingBandDto
 * @typedef {object} GradingBand
 * @property {string} grade
 * @property {number} minScore
 * @property {string | null} description
 */

export {}
