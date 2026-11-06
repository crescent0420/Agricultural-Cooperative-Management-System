// The backend's enrolment and contribution DTOs, written as JSDoc so editors can show them. Documentation only.

/** @typedef {'ENROLLED' | 'CANCELLED'} EnrolmentStatus  com.acms.enums.EnrolmentStatus */

/**
 * com.acms.dto.EnrolmentResponse
 * @typedef {object} Enrolment
 * @property {number} id
 * @property {number} offeringId
 * @property {number} programmeId
 * @property {string} programmeName
 * @property {number} seasonId
 * @property {string} seasonName
 * @property {number} farmerId
 * @property {string} membershipNumber
 * @property {string} farmerName
 * @property {number} hectares  copied from the offering when the farmer enrolled
 * @property {EnrolmentStatus} status
 * @property {string} enrolledAt  ISO instant
 * @property {string | null} cancelledAt
 * @property {number | null} cancelledById
 * @property {string | null} cancellationReason
 */

/**
 * com.acms.dto.ContributionResponse (the fields this module uses)
 * @typedef {object} Contribution
 * @property {number} id
 * @property {number} seasonId
 * @property {string} seasonName
 * @property {number} amountDue
 * @property {number} confirmedPaid
 * @property {number} paidPercent
 * @property {boolean} enrolmentAllowed  the backend's own answer to "at least 30% paid"
 * @property {boolean} inputCollectionAllowed
 */

export {}
