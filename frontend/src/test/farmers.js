import { vi } from 'vitest'
import * as authApi from '../api/auth'
import { saveSession } from '../auth/tokenStore'
import { ROLE_PERMISSIONS } from './rolePermissions'
import { registrar } from './users'

/** Test data shaped like the backend's farmer answers, and one demo user per role. */
export const huye = { id: 15, name: 'Huye', provinceId: 4, provinceName: 'Southern Province' }
export const musanze = { id: 21, name: 'Musanze', provinceId: 2, provinceName: 'Northern Province' }
export const huyeCoop = { id: 1, name: 'Huye Maize Growers Cooperative', districtId: 15, districtName: 'Huye' }

/** @type {import('../types/farmer').Farmer} */
export const uwase = {
  id: 7,
  membershipNumber: 'RCU-000007',
  nationalId: '1199080012345678',
  fullName: 'Uwase Marie',
  phoneNumber: '+250788123456',
  status: 'ACTIVE',
  cooperativeId: 1,
  cooperativeName: huyeCoop.name,
  districtId: 15,
  districtName: 'Huye',
}

export const uwaseSummary = {
  id: uwase.id,
  membershipNumber: uwase.membershipNumber,
  fullName: uwase.fullName,
  cooperativeId: 1,
  cooperativeName: huyeCoop.name,
  districtId: 15,
  districtName: 'Huye',
  status: 'ACTIVE',
  createdAt: '2026-10-20T08:30:00Z',
}

const base = { ...registrar, districtId: null, farmerId: null }
export const USERS = {
  registrar: { ...base, permissions: ROLE_PERMISSIONS.UNION_REGISTRAR },
  officer: {
    ...base,
    id: 5,
    username: 'demo.officer',
    role: 'FIELD_OFFICER',
    districtId: 15,
    permissions: ROLE_PERMISSIONS.FIELD_OFFICER,
  },
  agronomist: {
    ...base,
    id: 6,
    username: 'demo.agronomist',
    role: 'DISTRICT_AGRONOMIST',
    districtId: 15,
    permissions: ROLE_PERMISSIONS.DISTRICT_AGRONOMIST,
  },
  finance: {
    ...base,
    id: 3,
    username: 'demo.finance',
    role: 'FINANCE_OFFICER',
    permissions: ROLE_PERMISSIONS.FINANCE_OFFICER,
  },
  admin: { ...base, id: 1, username: 'demo.admin', role: 'SYSTEM_ADMIN', permissions: ROLE_PERMISSIONS.SYSTEM_ADMIN },
  farmer: { ...base, id: 20, username: 'uwase', role: 'FARMER', farmerId: 7, permissions: ROLE_PERMISSIONS.FARMER },
}

/** Pretends this user is logged in (the test file must call vi.mock('../../api/auth')). */
export function logInAs(user) {
  saveSession('token', 1800)
  vi.mocked(authApi.getCurrentUser).mockResolvedValue(user)
}
