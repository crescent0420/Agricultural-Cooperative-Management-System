import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { PageError } from './PageError'

function Broken() {
  throw new Error('Failed to fetch dynamically imported module')
}

describe('PageError (Phase 6, T3)', () => {
  it('replaces a crashed page with a way out instead of the developer screen', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {}) // React reports the thrown error; expected here
    const router = createMemoryRouter([{ path: '/', element: <Broken />, errorElement: <PageError /> }])
    render(<RouterProvider router={router} />)

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reload the page' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the start page' })).toHaveAttribute('href', '/')
    expect(screen.getByText('Failed to fetch dynamically imported module')).toBeInTheDocument()
    vi.restoreAllMocks()
  })
})
