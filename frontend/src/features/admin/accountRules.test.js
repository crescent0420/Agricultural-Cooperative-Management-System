import { describe, expect, it } from 'vitest'
import { conflictField, humanize, roleScope, scopeFields } from './accountRules'

describe('account rules (UserService.resolveScope)', () => {
  it('attaches district staff to a district, farmers to a farmer record, others to nothing', () => {
    expect(roleScope('FIELD_OFFICER')).toBe('district')
    expect(roleScope('DISTRICT_AGRONOMIST')).toBe('district')
    expect(roleScope('FARMER')).toBe('farmer')
    expect(roleScope('FINANCE_OFFICER')).toBe('none')
    expect(scopeFields('FIELD_OFFICER', '15', { id: 7 })).toEqual({ districtId: 15, farmerId: null })
    expect(scopeFields('FARMER', '15', { id: 7 })).toEqual({ districtId: null, farmerId: 7 })
    expect(scopeFields('SYSTEM_ADMIN', '15', { id: 7 })).toEqual({ districtId: null, farmerId: null })
  })

  it('places a 409 under the field it is about, and makes event codes readable', () => {
    expect(conflictField('Username is already in use.')).toBe('username')
    expect(conflictField('Email address is already in use.')).toBe('email')
    expect(conflictField('This farmer already has a login account.')).toBe('farmer')
    expect(conflictField('Something else.')).toBeNull()
    expect(humanize('LOGIN_FAILURE')).toBe('Login failure')
    expect(humanize(null)).toBe('—')
  })
})
