// The backend's permit DTOs, written as JSDoc so editors can show them. Documentation only.

/**
 * com.acms.dto.CollectionPermitResponse
 * @typedef {object} Permit
 * @property {number} id
 * @property {string} permitNumber  CP-XXXX-XXXX-XXXX
 * @property {number} enrolmentId
 * @property {number} farmerId
 * @property {string} membershipNumber
 * @property {string} farmerName
 * @property {string} programmeName
 * @property {string} seasonName
 * @property {string} distributionDate  "YYYY-MM-DD"
 * @property {string} issuedAt  ISO instant
 */

/**
 * com.acms.dto.PermitVerificationResponse
 * @typedef {object} PermitVerification
 * @property {boolean} valid
 * @property {string[]} problems
 * @property {Permit} permit
 */

/** @typedef {'ISSUED' | 'ALREADY_ISSUED' | 'REFUSED'} PermitOutcome */

/**
 * com.acms.dto.PermitAttemptResponse
 * @typedef {object} PermitAttempt
 * @property {number} id
 * @property {number} enrolmentId
 * @property {number} farmerId
 * @property {number} userId
 * @property {string} username
 * @property {PermitOutcome} outcome
 * @property {string | null} reason
 * @property {number | null} permitId
 * @property {string} attemptedAt
 */

export {}
