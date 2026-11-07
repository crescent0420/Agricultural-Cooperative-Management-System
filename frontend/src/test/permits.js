/** Test data shaped like the backend's permit answers. */
import { uwase } from './farmers'

export const permit = {
  id: 900,
  permitNumber: 'CP-AB2C-D3EF-GH4J',
  enrolmentId: 50,
  farmerId: uwase.id,
  membershipNumber: uwase.membershipNumber,
  farmerName: uwase.fullName,
  programmeName: 'Maize',
  seasonName: '2026B',
  distributionDate: '2026-12-15',
  issuedAt: '2026-11-07T10:00:00Z',
}

export const refusedAttempt = {
  id: 1,
  enrolmentId: 50,
  farmerId: uwase.id,
  userId: 20,
  username: 'uwase',
  outcome: 'REFUSED',
  reason: 'The contribution is not fully paid (40%).',
  permitId: null,
  attemptedAt: '2026-11-06T10:00:00Z',
}

export const issuedAttempt = {
  ...refusedAttempt,
  id: 2,
  outcome: 'ISSUED',
  reason: null,
  permitId: 900,
  attemptedAt: '2026-11-07T10:00:00Z',
}
