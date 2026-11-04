import type { CurrentUser, LoginResponse } from '../types/auth'

/** Test data shaped like the backend's answers for the demo accounts. */
export const registrar: CurrentUser = {
  id: 2,
  username: 'demo.registrar',
  email: 'registrar@example.test',
  role: 'UNION_REGISTRAR',
  emailVerified: true,
  mustChangePassword: false,
  districtId: null,
  farmerId: null,
  permissions: ['REFERENCE_READ', 'REFERENCE_MANAGE', 'FARMER_LIST', 'FARMER_MANAGE'],
}

export const newOfficer: CurrentUser = {
  ...registrar,
  id: 9,
  username: 'new.officer',
  role: 'FIELD_OFFICER',
  districtId: 15,
  mustChangePassword: true,
  permissions: [],
}

export const unlinkedFarmer: CurrentUser = {
  ...registrar,
  id: 12,
  username: 'self.registered',
  role: 'FARMER',
  permissions: ['REFERENCE_READ'],
}

export const loginAnswer = (mustChangePassword = false): LoginResponse => ({
  accessToken: 'test-token',
  tokenType: 'Bearer',
  expiresIn: 1800,
  mustChangePassword,
})
