import { request } from '@playwright/test'
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
    throw new Error(`No email to ${address} arrived in Mailpit within ${timeoutMs / 1000} s`)
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
