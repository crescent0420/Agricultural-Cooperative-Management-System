import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/errors'
import { applyFieldErrors } from './formErrors'

describe('applyFieldErrors', () => {
  it('puts backend field errors under known fields, converting list indexes', () => {
    const setError = vi.fn()
    const error = new ApiError(400, 'Validation failed', [
      { field: 'name', message: 'name must be at most 150 characters' },
      { field: 'bands[1].grade', message: 'grade must be at most 5 characters' },
      { field: 'somethingElse', message: 'ignored' },
    ])

    expect(applyFieldErrors(error, setError, ['name', 'bands'])).toBe(true)
    expect(setError).toHaveBeenCalledWith('name', { message: 'name must be at most 150 characters' })
    expect(setError).toHaveBeenCalledWith('bands.1.grade', { message: 'grade must be at most 5 characters' })
    expect(setError).toHaveBeenCalledTimes(2)
  })

  it('returns false when no field matches (e.g. a 409 duplicate), so the page shows the message instead', () => {
    expect(applyFieldErrors(new ApiError(409, "Cooperative 'X' already exists."), vi.fn(), ['name'])).toBe(false)
  })
})
