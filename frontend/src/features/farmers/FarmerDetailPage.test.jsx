import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import { logInAs, USERS, uwase } from '../../test/farmers'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/farmers')
vi.mock('../../api/reference')

const tabNames = () =>
  within(screen.getByRole('navigation', { name: 'Farmer' }))
    .getAllByRole('link')
    .map((link) => link.textContent)

describe('FarmerDetailPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(farmersApi.getFarmer).mockResolvedValue(uwase)
  })

  it('shows the farmer, with the tabs the role may open', async () => {
    logInAs(USERS.registrar)
    renderRoute('/farmers/7')
    expect(await screen.findByRole('heading', { name: 'Uwase Marie', level: 1 })).toBeInTheDocument()
    expect(await screen.findByText(uwase.nationalId)).toBeInTheDocument()
    expect(screen.getByText('+250788123456')).toBeInTheDocument()
    expect(tabNames()).toEqual(['Details', 'Status', 'Trainings', 'Missing prerequisites', 'Results'])
    expect(farmersApi.getFarmer).toHaveBeenCalledWith(7)
  })

  it('lets the Registrar edit the name and phone; an empty phone is sent as null', async () => {
    logInAs(USERS.registrar)
    vi.mocked(farmersApi.updateFarmer).mockResolvedValue({
      ...uwase,
      fullName: 'Uwase Marie Claire',
      phoneNumber: null,
    })
    renderRoute('/farmers/7/details')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit Uwase Marie' })
    const name = within(dialog).getByLabelText('Full name')
    await user.clear(name)
    await user.type(name, 'Uwase Marie Claire')
    await user.clear(within(dialog).getByLabelText('Phone number (optional)'))
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Farmer details saved')).toBeInTheDocument()
    expect(farmersApi.updateFarmer).toHaveBeenCalledWith(7, { fullName: 'Uwase Marie Claire', phoneNumber: null })
    // The saved values show immediately, without reloading the page (FR-FAR-04).
    expect(screen.getByRole('heading', { name: 'Uwase Marie Claire', level: 1 })).toBeInTheDocument()
  })

  it('shows a Field Officer the farmer read-only, with all reading tabs', async () => {
    logInAs(USERS.officer)
    renderRoute('/farmers/7/details')
    expect(await screen.findByRole('heading', { name: 'Uwase Marie', level: 1 })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(tabNames()).toEqual(['Details', 'Status', 'Trainings', 'Missing prerequisites', 'Results'])
  })

  it('shows "Not available to you" when the backend refuses the farmer (403, other district)', async () => {
    logInAs(USERS.officer)
    vi.mocked(farmersApi.getFarmer).mockRejectedValue(
      new ApiError(403, 'You do not have permission to perform this action.'),
    )
    renderRoute('/farmers/99')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })

  it.each([
    ['Finance Officer', USERS.finance],
    ['System Administrator', USERS.admin],
  ])('does not open a farmer for the %s (gap G6)', async (_, user) => {
    logInAs(user)
    renderRoute('/farmers/7')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    expect(farmersApi.getFarmer).not.toHaveBeenCalled()
  })

  describe('Status tab (FR-FAR-05)', () => {
    async function openStatusDialog(farmer) {
      vi.mocked(farmersApi.getFarmer).mockResolvedValue(farmer)
      logInAs(USERS.registrar)
      renderRoute('/farmers/7/status')
      const user = userEvent.setup()
      await user.click(await screen.findByRole('button', { name: 'Change status' }))
      const dialog = screen.getByRole('dialog')
      const options = within(within(dialog).getByLabelText('New status'))
        .getAllByRole('option')
        .slice(1)
        .map((o) => o.textContent)
      return { user, dialog, options }
    }

    it('offers only the changes the backend allows', async () => {
      expect((await openStatusDialog(uwase)).options).toEqual(['Suspended', 'Withdrawn', 'Retired'])
    })

    it('offers only Active to a withdrawn farmer', async () => {
      expect((await openStatusDialog({ ...uwase, status: 'WITHDRAWN' })).options).toEqual(['Active'])
    })

    it('offers no change at all for a retired farmer', async () => {
      vi.mocked(farmersApi.getFarmer).mockResolvedValue({ ...uwase, status: 'RETIRED' })
      logInAs(USERS.registrar)
      renderRoute('/farmers/7/status')
      expect(await screen.findByText('Final: the status can no longer be changed.')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Change status' })).not.toBeInTheDocument()
    })

    it('warns that retirement is final, then saves the change', async () => {
      vi.mocked(farmersApi.changeFarmerStatus).mockResolvedValue({ ...uwase, status: 'RETIRED' })
      const { user, dialog } = await openStatusDialog(uwase)
      await user.selectOptions(within(dialog).getByLabelText('New status'), 'Retired')
      expect(within(dialog).getByRole('alert')).toHaveTextContent('Retirement is final')
      await user.click(within(dialog).getByRole('button', { name: 'Retire farmer' }))

      expect(await screen.findByText('Status changed to Retired')).toBeInTheDocument()
      expect(farmersApi.changeFarmerStatus).toHaveBeenCalledWith(7, 'RETIRED')
      expect(screen.queryByRole('button', { name: 'Change status' })).not.toBeInTheDocument()
    })

    it('shows the backend refusal (422) in the dialog', async () => {
      vi.mocked(farmersApi.changeFarmerStatus).mockRejectedValue(
        new ApiError(422, 'Cannot change farmer status from ACTIVE to SUSPENDED.'),
      )
      const { user, dialog } = await openStatusDialog(uwase)
      await user.selectOptions(within(dialog).getByLabelText('New status'), 'Suspended')
      await user.click(within(dialog).getByRole('button', { name: 'Change status' }))
      expect(await within(dialog).findByRole('alert')).toHaveTextContent('Cannot change farmer status')
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    it('shows the status read-only to an Agronomist', async () => {
      logInAs(USERS.agronomist)
      renderRoute('/farmers/7/status')
      expect(await screen.findByText(/Can be changed to: Suspended, Withdrawn, Retired/)).toBeInTheDocument()
      await waitFor(() => expect(screen.queryByRole('button', { name: 'Change status' })).not.toBeInTheDocument())
    })
  })
})
