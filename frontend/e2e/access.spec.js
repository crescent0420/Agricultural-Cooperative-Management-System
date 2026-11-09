import { expect, test } from '@playwright/test'
import { canAccess } from '../src/navigation/access.js'
import { MENU } from '../src/navigation/menu.js'
import { ACCOUNTS } from './support/accounts'
import { Api } from './support/api'
import { logIn, mainMenu } from './support/ui'

// T2 (Phase 6): for every demo account, the menu, the pages and the backend agree on who may do what.
// URD §23: "allowed actions work, hidden actions are absent, and a direct API call for a hidden action
// returns 403". The permissions come from the backend (GET /auth/me and GET /permissions), never from here.

/** One read call per protected area, with the permission the backend requires for it. */
const PROBES = [
  { path: '/farmers', permission: 'FARMER_LIST' },
  { path: '/me/profile', permission: 'PROFILE_READ_OWN' },
  { path: '/me/enrolments', permission: 'ENROLMENT_READ_OWN' },
  { path: '/me/contributions', permission: 'CONTRIBUTION_READ_OWN' },
  { path: '/me/results', permission: 'RESULT_READ_OWN' },
  { path: '/me/permits', permission: 'PERMIT_READ_OWN' },
  { path: '/offerings/assigned-to-me', permission: 'OFFERING_READ_ASSIGNED' },
  { path: '/contributions', permission: 'CONTRIBUTION_LIST' },
  { path: '/permits/attempts', permission: 'PERMIT_ATTEMPT_READ' },
  { path: '/reports/farmers-per-cooperative', permission: 'REPORT_FARMERS_PER_COOPERATIVE' },
  { path: '/reports/outstanding-contributions', permission: 'REPORT_OUTSTANDING_CONTRIBUTIONS' },
  { path: '/users', permission: 'USER_MANAGE' },
  { path: '/audit-logs', permission: 'AUDIT_LOG_READ' },
]

const MENU_ITEMS = MENU.flatMap((group) => group.items)

let matrix
test.beforeAll(async () => {
  const admin = await Api.as(ACCOUNTS.admin)
  matrix = await admin.get('/permissions')
  await admin.dispose()
})

for (const username of Object.values(ACCOUNTS)) {
  test.describe(username, () => {
    test('the menu lists exactly the pages the role may open, and each of them opens without a refusal', async ({
      page,
    }) => {
      test.setTimeout(180_000) // about 16 pages per account
      const api = await Api.as(username)
      const me = await api.get('/auth/me')
      await api.dispose()
      // The login answer and the permission matrix agree.
      expect([...me.permissions].sort()).toEqual([...matrix.byRole[me.role]].sort())
      const has = (permission) => me.permissions.includes(permission)

      const refused = []
      page.on('response', (response) => {
        if (response.url().includes('/api/') && response.status() === 403) refused.push(response.url())
      })
      await logIn(page, username)
      const forbidden = page.getByRole('heading', { name: 'Not available to you' })

      for (const item of MENU_ITEMS) {
        const allowed = canAccess(item.requires, has)
        const link = mainMenu(page).getByRole('link', { name: item.label, exact: true })
        await expect(link, `menu link "${item.label}"`).toHaveCount(allowed ? 1 : 0)

        refused.length = 0
        if (allowed) {
          // Like a user: through the menu. The page must open, and none of its calls may be refused.
          await link.click()
          await expect(page).toHaveURL(new RegExp(`${item.to}(/|\\?|$)`))
          await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible()
          await page.waitForLoadState('networkidle')
          await expect(forbidden, `${item.to} should open`).toHaveCount(0)
          expect(refused, `${item.to} made calls the backend refused`).toEqual([])
        } else {
          // Not in the menu, so typed in the address bar.
          await page.goto(item.to)
          await expect(forbidden, `${item.to} should be refused`).toBeVisible()
        }
      }
    })

    test('the backend answers 403 to every call the role has no permission for', async () => {
      const api = await Api.as(username)
      const me = await api.get('/auth/me')
      const known = matrix.rows.map((row) => row.permission)
      for (const probe of PROBES) {
        expect(known, `${probe.permission} is a real permission`).toContain(probe.permission)
        const { status } = await api.call('GET', probe.path)
        if (me.permissions.includes(probe.permission)) {
          expect(status, `GET ${probe.path} with ${probe.permission}`).toBe(200)
        } else {
          expect(status, `GET ${probe.path} without ${probe.permission}`).toBe(403)
        }
      }
      await api.dispose()
    })
  })
}
