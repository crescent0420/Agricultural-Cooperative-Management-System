import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BackendStatus } from './BackendStatus'

describe('BackendStatus', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('calls /api/auth/me and reports the backend reachable when it answers 401', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    render(<BackendStatus />)

    expect(screen.getByText(/checking the backend connection/i)).toBeInTheDocument()
    expect(await screen.findByText(/backend reachable/i)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/me')
  })

  it('reports an unexpected status, e.g. the proxy failing with 500', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 500 })))

    render(<BackendStatus />)

    expect(await screen.findByText(/unexpected HTTP 500/i)).toBeInTheDocument()
  })

  it('reports no response when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    render(<BackendStatus />)

    expect(await screen.findByText(/no response/i)).toBeInTheDocument()
  })
})
