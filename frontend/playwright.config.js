import { defineConfig, devices } from '@playwright/test'

// End-to-end tests (Phase 6, T1-T4): a real browser drives the frontend, which talks to the REAL backend
// (Docker, database acms_e2e with the demo data). How to start everything: e2e/README.md.
export default defineConfig({
  testDir: './e2e',
  // One test at a time: the tests share one database, and some change it (a window opened, a payment confirmed).
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'e2e-report' }]],
  outputDir: 'e2e-results',
  globalSetup: './e2e/support/globalSetup.js',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:4173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  // Tests the production build (what will be deployed), served by "vite preview" on port 4173 with the same
  // /api proxy as "npm run dev". Built fresh for every run, so the tests always see the current code; and
  // no page has to be compiled on first use, which made the dev server too slow for the time limits.
  webServer: {
    command: 'npm run build && npx vite preview --port 4173 --strictPort',
    url: 'http://localhost:4173',
    reuseExistingServer: false,
    timeout: 120_000,
  },
})
