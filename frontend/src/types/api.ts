// TypeScript copies of the backend's common response types (com.acms.common).

/** One invalid field in a 400 answer (ApiError.FieldIssue). */
export interface FieldIssue {
  field: string
  message: string
}

/** The `error` part of a failed answer (com.acms.common.ApiError). */
export interface ApiErrorBody {
  status: number
  /** HTTP reason phrase, e.g. "Conflict". The human message is ApiResponse.message. */
  error: string
  path: string
  fieldErrors: FieldIssue[]
}

/** Every answer: { success, message, data | error, timestamp } (com.acms.common.ApiResponse). */
export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
  error?: ApiErrorBody
  timestamp: string
}

/** One page of a list (com.acms.common.PagedResponse). */
export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
  sort: string
}

/** ?page=&size=&sortBy=&direction= accepted by every paged list. sortBy must be one the endpoint allows. */
export interface PageParams {
  page: number
  size: number
  sortBy: string
  direction: 'asc' | 'desc'
}
