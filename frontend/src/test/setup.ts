// Runs before every test file: adds DOM matchers such as toBeInTheDocument(),
// and after each test unmounts rendered components and forgets any stored session.
import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach } from 'vitest'

// findBy... and waitFor wait up to 5 s (default 1 s) for the page to update, for the same reason as
// testTimeout in vite.config.ts: on a slow machine a correct page can take longer than 1 s to appear.
configure({ asyncUtilTimeout: 5000 })

afterEach(() => {
  cleanup()
  sessionStorage.clear()
})
