// The backend's payment and adjustment DTOs (the contribution is in types/enrolment.js). Documentation only.

/** @typedef {'PENDING' | 'CONFIRMED' | 'FAILED'} PaymentStatus */
/** @typedef {'MOBILE_MONEY' | 'BANK_TRANSFER'} PaymentMethod */

/**
 * com.acms.dto.PaymentResponse
 * @typedef {object} Payment
 * @property {number} id
 * @property {number} contributionId
 * @property {string} transactionReference
 * @property {number} amount
 * @property {PaymentMethod} method
 * @property {PaymentStatus} status
 * @property {string | null} providerReference
 * @property {number} requestedById
 * @property {string} requestedAt
 * @property {number | null} confirmedById
 * @property {string | null} confirmedAt
 * @property {number | null} failedById
 * @property {string | null} failedAt
 * @property {string | null} failureReason
 */

/**
 * com.acms.dto.ContributionAdjustmentResponse
 * @typedef {object} ContributionAdjustment
 * @property {number} id
 * @property {number} oldAmount
 * @property {number} newAmount
 * @property {string} reason
 * @property {number} adjustedById
 * @property {string} adjustedByUsername
 * @property {string} adjustedAt
 */

export {}
