import { expect, test } from '@playwright/test'
import { ACCOUNTS } from './support/accounts'
import { Api } from './support/api'
import { tamper } from './support/tokens'
import { logIn, mainMenu } from './support/ui'

// T3 (Phase 6): logging in and out, an expired or tampered token, refused and unknown pages.

test('a visitor without a session is sent to the login page', async ({ page }) => {
  await page.goto('/farmers')
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'Log in' })).toBeVisible()
})

test('a wrong password is refused in red and nobody is logged in', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill(ACCOUNTS.registrar)
  await page.getByLabel('Password', { exact: true }).fill('not-the-password-123')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)
})

test('a tampered token ends the session: back to the login page with "session expired"', async ({ page }) => {
  await logIn(page, ACCOUNTS.registrar)
  // Change one character of the stored token's signature, as if it had been edited or forged.
  const session = JSON.parse(await page.evaluate(() => sessionStorage.getItem('acms.session')))
  session.token = tamper(session.token)
  await page.evaluate((value) => sessionStorage.setItem('acms.session', value), JSON.stringify(session))
  await mainMenu(page).getByRole('link', { name: 'Farmers', exact: true }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('Your session has expired. Please log in again.')).toBeVisible()
})

test('logging out ends the session; pages need a new login', async ({ page }) => {
  await logIn(page, ACCOUNTS.finance)
  await page.getByRole('button', { name: 'Log out' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await page.goto('/contributions')
  await expect(page).toHaveURL(/\/login$/)
})

test('an unknown address shows "Page not found"', async ({ page }) => {
  await logIn(page, ACCOUNTS.registrar)
  await page.goto('/no-such-page')
  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible()
})

test('the backend answers 401 without a token and with an edited one', async () => {
  const anonymous = await Api.anonymous()
  expect((await anonymous.call('GET', '/farmers')).status).toBe(401)
  await anonymous.dispose()

  const registrar = await Api.as(ACCOUNTS.registrar)
  registrar.token = tamper(registrar.token)
  expect((await registrar.call('GET', '/farmers')).status).toBe(401)
  await registrar.dispose()
})

test("a farmer cannot read another farmer's results through the API (403)", async () => {
  const registrar = await Api.as(ACCOUNTS.registrar)
  const found = await registrar.get('/farmers', { name: 'Eric Niyonzima' })
  expect(found.content.length, 'the demo farmer Eric Niyonzima exists').toBeGreaterThan(0)
  const ericId = found.content[0].id
  await registrar.dispose()

  const aline = await Api.as(ACCOUNTS.farmer)
  expect((await aline.call('GET', `/farmers/${ericId}/results`)).status).toBe(403)
  await aline.dispose()
})
