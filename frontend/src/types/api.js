// The backend's common response shapes (com.acms.common), written as JSDoc so editors can show them.
// These are documentation only: nothing here runs.

/**
 * One invalid field in a 400 answer (ApiError.FieldIssue).
 * @typedef {object} FieldIssue
 * @property {string} field
 * @property {string} message
 */

/**
 * The `error` part of a failed answer (com.acms.common.ApiError).
 * @typedef {object} ApiErrorBody
 * @property {number} status
 * @property {string} error  HTTP reason phrase, e.g. "Conflict". The human message is ApiResponse.message.
 * @property {string} path
 * @property {FieldIssue[]} fieldErrors
 */

/**
 * Every answer: { success, message, data | error, timestamp } (com.acms.common.ApiResponse).
 * @template T
 * @typedef {object} ApiResponse
 * @property {boolean} success
 * @property {string} message
 * @property {T} [data]
 * @property {ApiErrorBody} [error]
 * @property {string} timestamp
 */

/**
 * One page of a list (com.acms.common.PagedResponse).
 * @template T
 * @typedef {object} PagedResponse
 * @property {T[]} content
 * @property {number} page  Counts from 0.
 * @property {number} size
 * @property {number} totalElements
 * @property {number} totalPages
 * @property {boolean} first
 * @property {boolean} last
 * @property {boolean} hasNext
 * @property {boolean} hasPrevious
 * @property {string} sort
 */

/**
 * ?page=&size=&sortBy=&direction= accepted by every paged list. sortBy must be one the endpoint allows.
 * @typedef {object} PageParams
 * @property {number} page
 * @property {number} size  1 to 100.
 * @property {string} sortBy
 * @property {'asc' | 'desc'} direction
 */

export {}
