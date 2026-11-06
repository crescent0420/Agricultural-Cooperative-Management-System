import { describe, expect, it } from 'vitest'
import { ROLE_PERMISSIONS } from '../test/rolePermissions'
import { visibleMenu } from './visibleMenu'

const labelsFor = (role) => visibleMenu(ROLE_PERMISSIONS[role]).flatMap((group) => group.items.map((i) => i.label))

/** Expected menus, from the URD's role-based access matrix (section 13). */
const EXPECTED = {
  FARMER: [
    'Dashboard',
    'My profile',
    'Offerings for me',
    'My enrolments',
    'My contributions',
    'My results',
    'My permits',
    'Seasons',
    'Reference data',
    'Grading scale',
    'Permission matrix',
  ],
  FIELD_OFFICER: [
    'Dashboard',
    'Farmers',
    'Offerings',
    'My assigned offerings',
    'Seasons',
    'Verify a permit',
    'Reports',
    'Reference data',
    'Grading scale',
    'Permission matrix',
  ],
  DISTRICT_AGRONOMIST: [
    'Dashboard',
    'Farmers',
    'Offerings',
    'Seasons',
    'Verify a permit',
    'Reports',
    'Reference data',
    'Grading scale',
    'Permission matrix',
  ],
  UNION_REGISTRAR: [
    'Dashboard',
    'Farmers',
    'Offerings',
    'Seasons',
    'Contributions',
    'Verify a permit',
    'Permit attempts',
    'Reports',
    'Reference data',
    'Grading scale',
    'Permission matrix',
  ],
  FINANCE_OFFICER: [
    'Dashboard',
    'Farmers',
    'Seasons',
    'Contributions',
    'Verify a permit',
    'Reports',
    'Reference data',
    'Grading scale',
    'Permission matrix',
  ],
  SYSTEM_ADMIN: [
    'Dashboard',
    'Farmers',
    'Offerings',
    'Seasons',
    'Verify a permit',
    'Permit attempts',
    'Reports',
    'Reference data',
    'Grading scale',
    'Users',
    'Audit log',
    'Permission matrix',
  ],
}

describe('visibleMenu', () => {
  it.each(Object.keys(EXPECTED))('shows %s exactly its menu', (role) => {
    expect(labelsFor(role)).toEqual(EXPECTED[role])
  })

  it('shows only the Dashboard to a user without permissions (one-time password not changed yet)', () => {
    expect(visibleMenu([]).flatMap((g) => g.items.map((i) => i.label))).toEqual(['Dashboard'])
  })

  it('never shows an empty group heading', () => {
    expect(visibleMenu(ROLE_PERMISSIONS.FINANCE_OFFICER).map((g) => g.label)).not.toContain('Administration')
  })
})
