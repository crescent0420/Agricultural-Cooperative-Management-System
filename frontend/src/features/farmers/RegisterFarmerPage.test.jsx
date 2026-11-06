import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as referenceApi from '../../api/reference'
import { huye, huyeCoop, logInAs, musanze, USERS, uwase } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/farmers')
vi.mock('../../api/reference')

async function fillForm(user, { phone = '' } = {}) {
  await user.type(screen.getByLabelText('Full name'), '  Uwase Marie ')
  await user.type(screen.getByLabelText('National ID'), uwase.nationalId)
  if (phone) await user.type(screen.getByLabelText('Phone number (optional)'), phone)
  await screen.findByRole('option', { name: 'Huye' })
  await user.selectOptions(screen.getByLabelText('District'), 'Huye')
  await screen.findByRole('option', { name: huyeCoop.name })
  await user.selectOptions(screen.getByLabelText('Cooperative'), huyeCoop.name)
}

describe('RegisterFarmerPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.registrar)
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(referenceApi.listCooperatives).mockResolvedValue(paged([huyeCoop]))
    vi.mocked(farmersApi.getFarmer).mockResolvedValue(uwase)
  })

  it('checks the fields like the backend before calling the API', async () => {
    renderRoute('/farmers/new')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('National ID'), '12345')
    await user.type(screen.getByLabelText('Phone number (optional)'), '07881')
    await user.click(screen.getByRole('button', { name: 'Register farmer' }))

    expect(await screen.findByText('Enter the full name.')).toBeInTheDocument()
    expect(screen.getByText('The national ID has exactly 16 digits.')).toBeInTheDocument()
    expect(screen.getByText('Use 9 to 15 digits, optionally starting with +.')).toBeInTheDocument()
    expect(screen.getByText('Choose a district.')).toBeInTheDocument()
    expect(screen.getByText('Choose a cooperative.')).toBeInTheDocument()
    expect(farmersApi.registerFarmer).not.toHaveBeenCalled()
  })

  it('asks for the district first, then lists only its cooperatives', async () => {
    renderRoute('/farmers/new')
    expect(await screen.findByLabelText('Cooperative')).toBeDisabled()
    await screen.findByRole('option', { name: 'Huye' })
    await userEvent.setup().selectOptions(screen.getByLabelText('District'), 'Huye')
    await waitFor(() => expect(screen.getByLabelText('Cooperative')).toBeEnabled())
    expect(referenceApi.listCooperatives).toHaveBeenCalledWith(expect.anything(), 15)
  })

  it('registers the farmer, shows the generated membership number and opens the farmer', async () => {
    vi.mocked(farmersApi.registerFarmer).mockResolvedValue(uwase)
    const { router } = renderRoute('/farmers/new')
    const user = userEvent.setup()
    await screen.findByLabelText('Full name')
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Register farmer' }))

    expect(await screen.findByText('Uwase Marie registered with membership number RCU-000007')).toBeInTheDocument()
    expect(farmersApi.registerFarmer).toHaveBeenCalledWith({
      fullName: 'Uwase Marie',
      nationalId: uwase.nationalId,
      phoneNumber: null, // an empty phone is sent as null
      cooperativeId: 1,
    })
    await waitFor(() => expect(router.state.location.pathname).toBe('/farmers/7/details'))
  })

  it('shows a duplicate national ID (409) under that field and stays on the form', async () => {
    vi.mocked(farmersApi.registerFarmer).mockRejectedValue(
      new ApiError(409, 'A farmer with this national ID is already registered.'),
    )
    const { router } = renderRoute('/farmers/new')
    const user = userEvent.setup()
    await screen.findByLabelText('Full name')
    await fillForm(user, { phone: '+250788123456' })
    await user.click(screen.getByRole('button', { name: 'Register farmer' }))

    expect(await screen.findByText('A farmer with this national ID is already registered.')).toBeInTheDocument()
    expect(screen.getByLabelText('National ID')).toHaveAttribute('aria-invalid', 'true')
    expect(farmersApi.registerFarmer).toHaveBeenCalledWith(expect.objectContaining({ phoneNumber: '+250788123456' }))
    expect(router.state.location.pathname).toBe('/farmers/new')
  })

  it('is not available to a Field Officer', async () => {
    logInAs(USERS.officer)
    renderRoute('/farmers/new')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
