import { apiClient, apiPost } from './client'

// Payments (URD FR-FIN-05, FR-FIN-06). A payment starts PENDING and changes once: to CONFIRMED (it now counts)
// or to FAILED. The transaction reference makes recording safe to repeat (the brief's network-failure case).

/**
 * FR-FIN-05. Sending the same transactionReference again with the same details returns the ORIGINAL payment
 * (200 "Already received") instead of creating a second one; with different details it answers 409.
 * An amount above what can still be paid answers 422.
 * @param {{ transactionReference: string, contributionId: number, amount: number,
 *           method: 'MOBILE_MONEY' | 'BANK_TRANSFER' }} payment
 * @returns {Promise<{ payment: object, created: boolean, message: string }>}
 */
export async function recordPayment(payment) {
  const response = await apiClient.post('/payments', payment)
  return { payment: response.data.data, created: response.status === 201, message: response.data.message }
}

/** FR-FIN-06: the provider's reference (max 100). Confirming again with the same reference changes nothing. */
export function confirmPayment(id, providerReference) {
  return apiPost(`/payments/${id}/confirm`, { providerReference })
}

/** FR-FIN-06: the reason (max 500) is required. */
export function failPayment(id, reason) {
  return apiPost(`/payments/${id}/fail`, { reason })
}

/**
 * A new transaction reference, made once when the Record payment dialog opens and kept for every retry.
 * Allowed by PaymentRequest: letters, digits, '.', '_', '-'; at most 64 characters.
 */
export function newTransactionReference(contributionId, now = new Date()) {
  const stamp = now.toISOString().replace(/[-:]/g, '').slice(0, 15) // 20261107T193412
  const random = Array.from(crypto.getRandomValues(new Uint8Array(4)), (b) => b.toString(16).padStart(2, '0')).join('')
  return `ACMS-${contributionId}-${stamp}-${random}`
}
