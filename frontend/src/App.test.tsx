import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the ACMS title', () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 401 })))

    render(<App />)

    expect(
      screen.getByRole('heading', { level: 1, name: /agricultural cooperative management system/i }),
    ).toBeInTheDocument()
  })
})
