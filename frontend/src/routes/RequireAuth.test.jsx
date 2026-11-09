import { act, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { notifyUnauthorized } from '../api/client'
import { saveSession } from '../auth/tokenStore'
import { renderRoute } from '../test/renderRoute'
import { registrar, unlinkedFarmer } from '../test/users'

vi.mock('../api/auth')

describe('RequireAuth', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('sends a visitor without a session to the login page', async () => {
    const { router } = renderRoute('/')
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
  })

  it('keeps a stored session after a page refresh', async () => {
    saveSession('token', 1800)
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(registrar)
    renderRoute('/')
    expect(await screen.findByText('Welcome, demo.registrar · Union Registrar')).toBeInTheDocument()
  })

  it('sends a farmer account without a farmer record to /not-linked', async () => {
    saveSession('token', 1800)
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(unlinkedFarmer)
    const { router } = renderRoute('/')
    expect(await screen.findByRole('heading', { name: 'Your account is not linked yet' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/not-linked')
  })

  it('returns to the login page with a message when the session expires (any 401)', async () => {
    saveSession('token', 1800)
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(registrar)
    renderRoute('/')
    await screen.findByText('Welcome, demo.registrar · Union Registrar')

    act(() => notifyUnauthorized())

    expect(await screen.findByText('Your session has expired. Please log in again.')).toBeInTheDocument()
    expect(sessionStorage.getItem('acms.session')).toBeNull()
  })

  it('shows a 404 page for an unknown address', async () => {
    renderRoute('/no-such-page')
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
