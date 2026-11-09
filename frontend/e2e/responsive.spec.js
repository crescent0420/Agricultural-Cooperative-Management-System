import { expect, test } from '@playwright/test'
import { ACCOUNTS } from './support/accounts'
import { logIn } from './support/ui'

// T4 (Phase 6): phone, tablet and desktop. No page may need sideways scrolling (wide tables scroll inside their
// own box, which is allowed), and the menu must be reachable at every width. Screenshots go to
// e2e-results/screens/ for a look by eye.

const SIZES = [
  { name: 'phone', width: 390, height: 844 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'desktop', width: 1280, height: 800 },
]

const PAGES = {
  [ACCOUNTS.registrar]: [
    '/',
    '/farmers',
    '/farmers/new',
    '/seasons',
    '/offerings',
    '/contributions',
    '/reports/farmers',
  ],
  [ACCOUNTS.farmer]: ['/', '/my/offerings', '/my/enrolments', '/my/contributions', '/my/permits'],
  [ACCOUNTS.admin]: ['/admin/users', '/admin/audit-log', '/permissions'],
}

for (const size of SIZES) {
  for (const [username, paths] of Object.entries(PAGES)) {
    test(`${size.name} (${size.width} px): ${username}'s pages fit the screen`, async ({ page }) => {
      await page.setViewportSize({ width: size.width, height: size.height })
      await logIn(page, username)
      for (const path of paths) {
        await page.goto(path)
        await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible()
        await page.waitForLoadState('networkidle')
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
        expect(overflow, `${path} is ${overflow} px wider than the screen`).toBeLessThanOrEqual(0)
        const file = `${size.name}-${username}${path.replaceAll('/', '_') || '_home'}.png`
        await page.screenshot({ path: `e2e-results/screens/${file}`, fullPage: true })
      }

      // The menu: always shown on desktop, behind the menu button on phone and tablet.
      if (size.width < 1024) {
        await page.getByRole('button', { name: 'Open menu' }).click()
        await expect(page.locator('#mobile-menu').getByRole('link', { name: 'Dashboard' })).toBeVisible()
        await page.keyboard.press('Escape')
      } else {
        await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
      }
    })
  }
}
