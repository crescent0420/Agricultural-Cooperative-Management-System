import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import { ApiError, SERVER_UNREACHABLE } from '../../api/errors'
import { renderRoute } from '../../test/renderRoute'
import { loginAnswer, registrar } from '../../test/users'

vi.mock('../../api/auth')

async function logIn(username: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Username'), username)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Log in' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('asks for both fields before calling the API', async () => {
    renderRoute('/login')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Enter your username.')).toBeInTheDocument()
    expect(screen.getByText('Enter your password.')).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('logs in, loads the user and opens the start page', async () => {
    vi.mocked(authApi.login).mockResolvedValue(loginAnswer())
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(registrar)
    renderRoute('/login')

    await logIn('demo.registrar', 'correct-password')

    expect(await screen.findByRole('heading', { name: 'Welcome, demo.registrar' })).toBeInTheDocument()
    expect(screen.getByTestId('role-line')).toHaveTextContent('Union Registrar')
    expect(authApi.login).toHaveBeenCalledWith('demo.registrar', 'correct-password')
    expect(sessionStorage.getItem('acms.session')).toContain('test-token')
  })

  it('shows the backend message for wrong credentials (401) and stays on the page', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'Invalid username or password.'))
    renderRoute('/login')

    await logIn('demo.registrar', 'wrong-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password.')
    expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(sessionStorage.getItem('acms.session')).toBeNull()
  })

  it('shows the email-not-verified message (422)', async () => {
    const message = 'Please confirm your email address first, using the link in your account email.'
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(422, message))
    renderRoute('/login')

    await logIn('new.farmer', 'one-time-password')

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
  })

  it('tells the user when the server cannot be reached', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(0, SERVER_UNREACHABLE))
    renderRoute('/login')

    await logIn('demo.registrar', 'any-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('could not be reached')
  })
})
