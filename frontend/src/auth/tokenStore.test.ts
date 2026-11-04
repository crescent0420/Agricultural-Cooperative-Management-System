import { afterEach, describe, expect, it, vi } from 'vitest'
import { clearSession, getToken, saveSession } from './tokenStore'

describe('tokenStore', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('keeps the token in sessionStorage until it is cleared', () => {
    saveSession('abc', 1800)
    expect(getToken()).toBe('abc')
    expect(sessionStorage.getItem('acms.session')).toContain('abc')
    clearSession()
    expect(getToken()).toBeNull()
  })

  it('forgets an expired token', () => {
    vi.useFakeTimers()
    saveSession('abc', 60)
    vi.advanceTimersByTime(61_000)
    expect(getToken()).toBeNull()
    expect(sessionStorage.getItem('acms.session')).toBeNull()
  })
})
