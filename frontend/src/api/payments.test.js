import { describe, expect, it } from 'vitest'
import { newTransactionReference } from './payments'

describe('newTransactionReference (FR-FIN-05)', () => {
  it('makes a reference the backend accepts, different every time', () => {
    const now = new Date('2026-11-07T19:34:12Z')
    const a = newTransactionReference(70, now)
    const b = newTransactionReference(70, now)
    expect(a).toMatch(/^ACMS-70-20261107T193412-[0-9a-f]{8}$/)
    expect(a).toMatch(/^[A-Za-z0-9._-]{1,64}$/) // PaymentRequest.transactionReference
    expect(a).not.toBe(b)
  })
})
