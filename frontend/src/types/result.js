// The backend's inspection and result DTOs, written as JSDoc so editors can show them. Documentation only.

/**
 * com.acms.dto.InspectionResponse
 * @typedef {object} Inspection
 * @property {number} id
 * @property {number} offeringId
 * @property {string} name
 * @property {number} maxScore
 * @property {number} weight  percent
 */

/**
 * com.acms.dto.InspectionListResponse
 * @typedef {object} InspectionList
 * @property {number} offeringId
 * @property {Inspection[]} inspections
 * @property {number} totalWeight
 * @property {boolean} weightsComplete  totalWeight is exactly 100
 * @property {boolean} frozen  a result has been submitted: the inspections can no longer change
 */

/** @typedef {'DRAFT' | 'SUBMITTED' | 'PUBLISHED'} ResultStatus */

/**
 * com.acms.dto.ScoreLine
 * @typedef {object} ScoreLine
 * @property {number} inspectionId
 * @property {string} inspectionName
 * @property {number} maxScore
 * @property {number} weight
 * @property {number | null} originalScore  as the Field Officer entered it
 * @property {number | null} currentScore  with corrections applied: the value that counts
 * @property {boolean} corrected
 */

/**
 * com.acms.dto.ResultResponse
 * @typedef {object} Result
 * @property {number} id
 * @property {number} enrolmentId
 * @property {number} offeringId
 * @property {string} programmeName
 * @property {string} seasonName
 * @property {number} farmerId
 * @property {string} membershipNumber
 * @property {string} farmerName
 * @property {ResultStatus} status
 * @property {ScoreLine[]} scores
 * @property {number | null} finalScore
 * @property {string | null} grade
 * @property {string | null} submittedAt
 * @property {string | null} publishedAt
 * @property {number} correctionCount
 */

/**
 * com.acms.dto.ResultCorrectionResponse
 * @typedef {object} ResultCorrection
 * @property {number} id
 * @property {number} inspectionId
 * @property {string} inspectionName
 * @property {number} originalScore
 * @property {number} correctedScore
 * @property {number} originalFinalScore
 * @property {number} correctedFinalScore
 * @property {string} originalGrade
 * @property {string} correctedGrade
 * @property {string} reason
 * @property {number} correctedById
 * @property {string} correctedByUsername
 * @property {string} correctedAt
 */

export {}
