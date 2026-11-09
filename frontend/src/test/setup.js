// Runs before every test file: adds DOM matchers such as toBeInTheDocument(),
// and after each test unmounts rendered components and forgets any stored session.
import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach } from 'vitest'

// findBy... and waitFor wait up to 5 s (default 1 s) for the page to update, for the same reason as
// testTimeout in vite.config.js: on a slow machine a correct page can take longer than 1 s to appear.
// 10 s (was 5 s, 9 Nov 2026): with all 48 test files sharing the processor, correct pages took longer than 5 s
// on the developer's laptop; the same tests passed when run alone. A wait ends as soon as the element appears.
configure({ asyncUtilTimeout: 10_000 })

afterEach(() => {
  cleanup()
  sessionStorage.clear()
})
