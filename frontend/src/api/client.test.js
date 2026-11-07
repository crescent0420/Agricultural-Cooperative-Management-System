import { AxiosError } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { saveSession } from '../auth/tokenStore'
import { apiClient, apiGet, apiPost, setUnauthorizedHandler } from './client'
import { ApiError } from './errors'

/** Replaces the network with a function that answers every request. */
function answer(status, data) {
  const adapter = vi.fn(async (config) => {
    const response = { status, statusText: '', headers: {}, config, data }
    if (status >= 400) throw new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, response)
    return response
  })
  apiClient.defaults.adapter = adapter
  return adapter
}

const envelope401 = { success: false, message: 'Authentication is required or the access token is invalid.' }

describe('api client', () => {
  const originalAdapter = apiClient.defaults.adapter
  const onUnauthorized = vi.fn()

  beforeEach(() => setUnauthorizedHandler(onUnauthorized))
  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
    setUnauthorizedHandler(null)
    onUnauthorized.mockReset()
  })

  it('sends the stored token and returns only `data` from the envelope', async () => {
    saveSession('secret-token', 1800)
    const adapter = answer(200, { success: true, message: 'OK', data: { id: 7 } })

    await expect(apiGet('/farmers/7')).resolves.toEqual({ id: 7 })
    const sent = adapter.mock.calls[0][0]
    expect(sent.url).toBe('/farmers/7')
    expect(sent.headers.get('Authorization')).toBe('Bearer secret-token')
  })

  it('returns undefined for 204 No Content', async () => {
    saveSession('secret-token', 1800)
    answer(204, '')
    await expect(apiPost('/auth/change-password', {})).resolves.toBeUndefined()
  })

  it('reports an expired session when a logged-in call answers 401', async () => {
    saveSession('old-token', 1800)
    answer(401, envelope401)
    await expect(apiGet('/auth/me')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('reports an expired session when the token ran out in the browser and none was sent (regression)', async () => {
    saveSession('old-token', 1800)
    vi.useFakeTimers({ now: Date.now() + 31 * 60 * 1000, toFake: ['Date'] })
    try {
      const adapter = answer(401, envelope401)
      await expect(apiPost('/offerings/12/inspections', {})).rejects.toBeInstanceOf(ApiError)
      expect(adapter.mock.calls[0][0].headers.get('Authorization')).toBeFalsy()
      expect(onUnauthorized).toHaveBeenCalledOnce()
    } finally {
      vi.useRealTimers()
    }
  })

  it('does NOT report an expired session for a wrong password at login', async () => {
    answer(401, { success: false, message: 'Invalid username or password.' })
    await expect(apiPost('/auth/login', {})).rejects.toMatchObject({
      status: 401,
      message: 'Invalid username or password.',
    })
    expect(onUnauthorized).not.toHaveBeenCalled()
  })
})
