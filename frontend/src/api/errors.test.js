import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it } from 'vitest'
import { ApiError, SERVER_UNREACHABLE, toApiError } from './errors'

const config = { headers: new AxiosHeaders() }
const failure = (status, data) =>
  new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, { status, statusText: '', headers: {}, config, data })

describe('toApiError', () => {
  it('reads the message and field errors from the backend envelope', () => {
    const error = toApiError(
      failure(400, {
        success: false,
        message: 'Validation failed',
        error: {
          status: 400,
          error: 'Bad Request',
          path: '/api/farmers',
          fieldErrors: [{ field: 'nationalId', message: 'nationalId must be exactly 16 digits' }],
        },
      }),
    )
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Validation failed')
    expect(error.fieldMessage('nationalId')).toBe('nationalId must be exactly 16 digits')
  })

  it('treats no answer at all as "server unreachable" (status 0)', () => {
    const error = toApiError(new AxiosError('Network Error', 'ERR_NETWORK', config))
    expect(error.status).toBe(0)
    expect(error.message).toBe(SERVER_UNREACHABLE)
  })

  it('treats a 5xx without the envelope (e.g. the dev proxy failing) as "server unreachable"', () => {
    expect(toApiError(failure(502, '')).message).toBe(SERVER_UNREACHABLE)
  })
})
