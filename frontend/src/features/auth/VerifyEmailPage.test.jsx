import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import { ApiError } from '../../api/errors'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')

describe('Verify email page (the link in an account email)', () => {
  beforeEach(() => {
    vi.mocked(authApi.getCurrentUser).mockRejectedValue(new ApiError(401, 'Authentication is required.'))
  })

  it('confirms the address once and tells the user to log in with the one-time password', async () => {
    vi.mocked(authApi.verifyEmail).mockResolvedValue(
      'Your email address is verified. You can now log in with the one-time password from the email.',
    )
    renderRoute('/verify-email?token=abc123')

    expect(await screen.findByRole('heading', { name: 'Email address confirmed' })).toBeInTheDocument()
    expect(screen.getByText(/Your email address is verified/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Go to log in' })).toHaveAttribute('href', '/login')
    expect(authApi.verifyEmail).toHaveBeenCalledExactlyOnceWith('abc123')
  })

  it("shows the backend's refusal in red for a used or expired link, and how to get a new one", async () => {
    vi.mocked(authApi.verifyEmail).mockRejectedValue(
      new ApiError(400, 'This verification link is invalid, already used or expired.'),
    )
    renderRoute('/verify-email?token=used')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('This verification link is invalid, already used or expired.')
    expect(screen.getByText(/ask your system administrator to send you a new email/)).toBeInTheDocument()
    expect(authApi.verifyEmail).toHaveBeenCalledOnce()
  })

  it('says the link is incomplete, without calling the backend, when there is no token', async () => {
    renderRoute('/verify-email')

    expect(await screen.findByText(/This link is incomplete/)).toBeInTheDocument()
    expect(authApi.verifyEmail).not.toHaveBeenCalled()
  })
})
