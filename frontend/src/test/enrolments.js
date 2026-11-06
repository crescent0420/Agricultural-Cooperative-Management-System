/** Test data shaped like the backend's enrolment and contribution answers. */
import { uwase } from './farmers'

/** @type {import('../types/enrolment').Enrolment} */
export const uwaseMaize = {
  id: 50,
  offeringId: 12,
  programmeId: 30,
  programmeName: 'Maize',
  seasonId: 3,
  seasonName: '2026B',
  farmerId: uwase.id,
  membershipNumber: uwase.membershipNumber,
  farmerName: uwase.fullName,
  hectares: 0.5,
  status: 'ENROLLED',
  enrolledAt: '2026-09-03T08:00:00Z',
  cancelledAt: null,
  cancelledById: null,
  cancellationReason: null,
}

export const uwaseBeansCancelled = {
  ...uwaseMaize,
  id: 51,
  offeringId: 13,
  programmeId: 31,
  programmeName: 'Beans',
  status: 'CANCELLED',
  cancelledAt: '2026-09-05T10:00:00Z',
  cancelledById: 20,
  cancellationReason: 'Planting maize only this season',
}

/** @type {import('../types/enrolment').Contribution} */
export const contribution2026B = {
  id: 70,
  farmerId: uwase.id,
  membershipNumber: uwase.membershipNumber,
  farmerName: uwase.fullName,
  seasonId: 3,
  seasonName: '2026B',
  amountDue: 50000,
  confirmedPaid: 20000,
  pendingAmount: 0,
  outstanding: 30000,
  paidPercent: 40,
  enrolmentAllowed: true,
  inputCollectionAllowed: false,
  createdAt: '2026-08-20T08:00:00Z',
  updatedAt: '2026-08-25T08:00:00Z',
}
