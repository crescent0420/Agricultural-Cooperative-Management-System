import axios from 'axios'

/**
 * Every failed API call becomes one ApiError, whatever went wrong:
 * the backend's error envelope (400, 401, 403, 404, 409, 422, 500) or no answer at all (status 0).
 */
export class ApiError extends Error {
  status
  fieldErrors

  constructor(status, message, fieldErrors = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }

  /** The message for one form field, if the backend reported one (400 validation). */
  fieldMessage(field) {
    return this.fieldErrors.find((issue) => issue.field === field)?.message
  }
}

export const SERVER_UNREACHABLE =
  'The ACMS server could not be reached. Check your connection, or try again in a moment.'

export function toApiError(error) {
  if (error instanceof ApiError) return error
  if (axios.isAxiosError(error)) {
    const response = error.response
    if (!response) return new ApiError(0, SERVER_UNREACHABLE)
    const body = response.data
    const message =
      typeof body?.message === 'string' && body.message.length > 0
        ? body.message
        : response.status >= 500
          ? SERVER_UNREACHABLE
          : `The request failed (HTTP ${response.status}).`
    return new ApiError(response.status, message, body?.error?.fieldErrors ?? [])
  }
  return new ApiError(0, 'Something went wrong. Please try again.')
}
