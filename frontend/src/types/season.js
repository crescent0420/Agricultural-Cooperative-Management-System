// The backend's season and offering DTOs, written as JSDoc so editors can show them. Documentation only.

/**
 * com.acms.dto.SeasonResponse
 * @typedef {object} Season
 * @property {number} id
 * @property {string} name
 * @property {string} startsOn  "YYYY-MM-DD"
 * @property {string} endsOn
 * @property {number} maxLandHectares  the most land one farmer may enrol in this season
 * @property {string} distributionDate  inside the season
 * @property {boolean} enrolmentOpen
 * @property {number | null} openWindowId
 * @property {string} createdAt  ISO instant
 * @property {string} updatedAt
 */

/**
 * com.acms.dto.EnrolmentWindowResponse
 * @typedef {object} EnrolmentWindow
 * @property {number} id
 * @property {number} seasonId
 * @property {string} seasonName
 * @property {'OPEN' | 'CLOSED'} status
 * @property {string} openedAt  ISO instant
 * @property {string} openedBy  username
 * @property {string | null} closedAt
 * @property {string | null} closedBy
 */

/**
 * com.acms.dto.ProgrammeOfferingResponse
 * @typedef {object} Offering
 * @property {number} id
 * @property {number} programmeId
 * @property {string} programmeName
 * @property {number} districtId
 * @property {string} districtName
 * @property {number} seasonId
 * @property {string} seasonName
 * @property {number} fieldOfficerId
 * @property {string} fieldOfficerUsername
 * @property {number} hectaresPerParticipant
 * @property {number} maxParticipants
 * @property {number} enrolledCount
 * @property {number} remainingPlaces
 * @property {string} createdAt
 * @property {string} updatedAt
 */

/**
 * com.acms.dto.FieldOfficerOptionResponse (G8)
 * @typedef {object} FieldOfficerOption
 * @property {number} id
 * @property {string} username
 * @property {number} districtId
 * @property {string} districtName
 */

export {}
