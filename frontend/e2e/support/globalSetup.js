import { request } from '@playwright/test'
import { ACCOUNTS } from './accounts'
import { API_URL, DEMO_PASSWORD, ENV_FILE } from './env'

/**
 * Runs once before the tests: stops early, with a clear message, when the backend is not running with the
 * demo data, instead of letting every test fail on its own.
 */
export default async function globalSetup() {
  if (!DEMO_PASSWORD) {
    throw new Error(`DEMO_SEED_PASSWORD is empty. Set it in ${ENV_FILE} (see e2e/README.md).`)
  }
  const context = await request.newContext()
  try {
    let response
    try {
      response = await context.post(`${API_URL}/auth/login`, {
        data: { username: ACCOUNTS.admin, password: DEMO_PASSWORD },
      })
    } catch {
      throw new Error(`The backend does not answer at ${API_URL}. Start it as described in e2e/README.md.`)
    }
    if (!response.ok()) {
      throw new Error(
        `Logging in as ${ACCOUNTS.admin} failed (${response.status()}). Is the backend using the acms_e2e ` +
          'database with the demo data, and is DEMO_SEED_PASSWORD the one it was created with? See e2e/README.md.',
      )
    }
  } finally {
    await context.dispose()
  }
}
