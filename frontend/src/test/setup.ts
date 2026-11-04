// Runs before every test file: adds DOM matchers such as toBeInTheDocument(),
// and after each test unmounts rendered components and forgets any stored session.
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
  sessionStorage.clear()
})
