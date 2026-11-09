import { expect } from '@playwright/test'
import { DEMO_PASSWORD } from './env'

/** Logs in through the login page, like a user, and waits for the dashboard. */
export async function logIn(page, username, password = DEMO_PASSWORD) {
  await page.goto('/login')
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('heading', { name: 'Dashboard', level: 1 })).toBeVisible()
}

/** The main menu (desktop width). */
export const mainMenu = (page) => page.getByRole('navigation', { name: 'Main' })
