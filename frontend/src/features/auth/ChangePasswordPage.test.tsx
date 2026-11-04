import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../../api/auth'
import * as referenceApi from '../../api/reference'
import { ApiError } from '../../api/errors'
import { saveSession } from '../../auth/tokenStore'
import { renderRoute } from '../../test/renderRoute'
import { loginAnswer, newOfficer } from '../../test/users'

vi.mock('../../api/auth')
vi.mock('../../api/reference')

describe('ChangePasswordPage (forced, after a one-time password)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    saveSession('one-time-token', 1800)
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(newOfficer)
    vi.mocked(referenceApi.getDistrict).mockResolvedValue({ id: 15, name: 'Huye', provinceId: 4, provinceName: 'Southern Province' })
  })

  it('sends a user who must change the password there, whatever page they open', async () => {
    renderRoute('/')
    expect(await screen.findByRole('heading', { name: 'Choose your own password' })).toBeInTheDocument()
  })

  it('checks length and confirmation before calling the API', async () => {
    renderRoute('/change-password')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('One-time password'), 'OneTime-123')
    await user.type(screen.getByLabelText('New password'), 'short')
    await user.type(screen.getByLabelText('Confirm new password'), 'different')
    await user.click(screen.getByRole('button', { name: 'Save new password' }))

    expect(await screen.findByText('Use at least 12 characters.')).toBeInTheDocument()
    expect(screen.getByText('The two new passwords do not match.')).toBeInTheDocument()
    expect(authApi.changePassword).not.toHaveBeenCalled()
  })

  it('changes the password, logs in again with the new one, and opens the start page', async () => {
    vi.mocked(authApi.changePassword).mockResolvedValue(undefined)
    vi.mocked(authApi.login).mockResolvedValue(loginAnswer())
    renderRoute('/change-password')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('One-time password'), 'OneTime-123')
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({ ...newOfficer, mustChangePassword: false, permissions: ['FARMER_LIST'] })
    await user.type(screen.getByLabelText('New password'), 'My-New-Password-2026')
    await user.type(screen.getByLabelText('Confirm new password'), 'My-New-Password-2026')
    await user.click(screen.getByRole('button', { name: 'Save new password' }))

    expect(await screen.findByText('Your password has been changed.')).toBeInTheDocument()
    expect(authApi.changePassword).toHaveBeenCalledWith('OneTime-123', 'My-New-Password-2026')
    expect(authApi.login).toHaveBeenCalledWith('new.officer', 'My-New-Password-2026')
  })

  it('shows the backend refusal, e.g. a wrong current password (400)', async () => {
    vi.mocked(authApi.changePassword).mockRejectedValue(new ApiError(400, 'Current password is incorrect.'))
    renderRoute('/change-password')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('One-time password'), 'Wrong-one-time')
    await user.type(screen.getByLabelText('New password'), 'My-New-Password-2026')
    await user.type(screen.getByLabelText('Confirm new password'), 'My-New-Password-2026')
    await user.click(screen.getByRole('button', { name: 'Save new password' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Current password is incorrect.')
  })
})
