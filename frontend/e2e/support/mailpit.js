import { request } from '@playwright/test'
import { Api } from './api'
import { MAILPIT_URL } from './env'

/**
 * The newest email Mailpit caught for this address (the backend sends account emails there in Docker).
 * Waits up to `timeoutMs` for it to arrive. Returns { subject, text }.
 */
export async function latestEmailTo(address, { timeoutMs = 15_000 } = {}) {
  const context = await request.newContext()
  try {
    const deadline = Date.now() + timeoutMs
    while (Date.now() < deadline) {
      const search = await context.get(`${MAILPIT_URL}/api/v1/search`, { params: { query: `to:"${address}"` } })
      const messages = (await search.json()).messages ?? []
      if (messages.length > 0) {
        const message = await (await context.get(`${MAILPIT_URL}/api/v1/message/${messages[0].ID}`)).json()
        return { subject: message.Subject, text: message.Text }
      }
      await new Promise((resolve) => setTimeout(resolve, 500))
    }
    // Say whether Mailpit answers at all and what it holds, to tell "no email sent" from "Mailpit not reached".
    const all = await (await context.get(`${MAILPIT_URL}/api/v1/messages`, { params: { limit: 3 } })).json()
    const newest = (all.messages ?? []).map((m) => m.To?.map((to) => to.Address).join(', ')).join(' | ')
    throw new Error(
      `No email to ${address} arrived in Mailpit within ${timeoutMs / 1000} s. Mailpit holds ${all.total ?? 0} ` +
        `emails; the newest are to: ${newest || 'nobody'}. Check the backend log for mail errors.`,
    )
  } finally {
    await context.dispose()
  }
}

/** The one-time password in an account email (the same pattern as the backend's IntegrationTestBase). */
export function oneTimePassword(email) {
  const match = email.text.match(/One-time password: (\S+)/)
  if (!match) throw new Error(`No one-time password in the email "${email.subject}"`)
  return match[1]
}

/**
 * Opens the verification link in an account email, as the user would by clicking it. Until then the backend
 * refuses to log the account in ("Please confirm your email address first").
 */
export async function verifyEmail(email) {
  const match = email.text.match(/verify-email\?token=(\S+)/)
  if (!match) throw new Error(`No verification link in the email "${email.subject}"`)
  const anonymous = await Api.anonymous()
  try {
    await anonymous.ok('GET', '/auth/verify-email', { params: { token: match[1] } })
  } finally {
    await anonymous.dispose()
  }
}
