/** Test data shaped like the backend's contribution, payment and adjustment answers. */
import { contribution2026B } from './enrolments'

export const contribution = contribution2026B // id 70: Uwase Marie, 2026B, 50,000 due, 20,000 confirmed

export const confirmedPayment = {
  id: 301,
  contributionId: 70,
  transactionReference: 'ACMS-70-20260825T080000-aa11bb22',
  amount: 20000,
  method: 'MOBILE_MONEY',
  status: 'CONFIRMED',
  providerReference: 'MM-123456',
  requestedById: 3,
  requestedAt: '2026-08-25T08:00:00Z',
  confirmedById: 3,
  confirmedAt: '2026-08-25T09:00:00Z',
  failedById: null,
  failedAt: null,
  failureReason: null,
}

export const pendingPayment = {
  ...confirmedPayment,
  id: 302,
  transactionReference: 'ACMS-70-20261101T080000-cc33dd44',
  amount: 10000,
  method: 'BANK_TRANSFER',
  status: 'PENDING',
  providerReference: null,
  requestedAt: '2026-11-01T08:00:00Z',
  confirmedById: null,
  confirmedAt: null,
}

export const adjustment = {
  id: 9,
  oldAmount: 45000,
  newAmount: 50000,
  reason: 'Fertiliser price update',
  adjustedById: 3,
  adjustedByUsername: 'demo.finance',
  adjustedAt: '2026-08-21T08:00:00Z',
}
