import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { notifyUnauthorized } from '../api/client'
import { saveSession } from '../auth/tokenStore'
import { renderRoute } from '../test/renderRoute'
import { loginAnswer, registrar, unlinkedFarmer } from '../test/users'

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

  describe('the page after logging in again', () => {
    // /seasons: a page the registrar may not open, so it needs no API answers; only the address matters here.
    const openSeasonsAsRegistrar = async () => {
      saveSession('token', 1800)
      vi.mocked(authApi.getCurrentUser).mockResolvedValue(registrar)
      vi.mocked(authApi.login).mockResolvedValue(loginAnswer())
      const view = renderRoute('/seasons')
      await screen.findByRole('button', { name: 'Log out' })
      return view
    }
    const logInAgain = async (user) => {
      await user.type(await screen.findByLabelText('Username'), 'demo.registrar')
      await user.type(screen.getByLabelText('Password', { exact: true }), 'Secret-123')
      await user.click(screen.getByRole('button', { name: 'Log in' }))
    }

    it('after the session expired: back to the page the user was on', async () => {
      const user = userEvent.setup()
      const { router } = await openSeasonsAsRegistrar()
      act(() => notifyUnauthorized())
      await screen.findByText('Your session has expired. Please log in again.')
      await logInAgain(user)
      await vi.waitFor(() => expect(router.state.location.pathname).toBe('/seasons'))
    })

    it("after logging out: the Dashboard, never the previous user's page", async () => {
      const user = userEvent.setup()
      const { router } = await openSeasonsAsRegistrar()
      await user.click(screen.getByRole('button', { name: 'Log out' }))
      await logInAgain(user)
      expect(await screen.findByText('Welcome, demo.registrar · Union Registrar')).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/')
    })
  })

  it('shows a 404 page for an unknown address', async () => {
    renderRoute('/no-such-page')
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
