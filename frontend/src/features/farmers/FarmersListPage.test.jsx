import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as farmersApi from '../../api/farmers'
import * as referenceApi from '../../api/reference'
import { huye, huyeCoop, logInAs, musanze, USERS, uwaseSummary } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/farmers')
vi.mock('../../api/reference')

const DEFAULT_PAGE = { page: 0, size: 20, sortBy: 'fullName', direction: 'asc' }

describe('FarmersListPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(referenceApi.listCooperatives).mockResolvedValue(paged([huyeCoop]))
    vi.mocked(farmersApi.searchFarmers).mockResolvedValue(paged([uwaseSummary]))
  })

  it('lists farmers with links, and sends the search, filters and sorting to the API', async () => {
    logInAs(USERS.registrar)
    renderRoute('/farmers')
    expect(await screen.findByRole('link', { name: 'Uwase Marie' })).toHaveAttribute('href', '/farmers/7')
    expect(screen.getByText('RCU-000007')).toBeInTheDocument()
    expect(farmersApi.searchFarmers).toHaveBeenCalledWith(DEFAULT_PAGE, {
      name: '',
      membershipNumber: '',
      cooperativeId: undefined,
      status: '',
    })

    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Name'), '  Uwase ')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() =>
      expect(farmersApi.searchFarmers).toHaveBeenLastCalledWith(
        DEFAULT_PAGE,
        expect.objectContaining({ name: 'Uwase' }),
      ),
    )

    await user.selectOptions(screen.getByLabelText('Status'), 'Suspended')
    await waitFor(() =>
      expect(farmersApi.searchFarmers).toHaveBeenLastCalledWith(
        DEFAULT_PAGE,
        expect.objectContaining({ name: 'Uwase', status: 'SUSPENDED' }),
      ),
    )

    await screen.findByRole('option', { name: 'Huye' })
    await user.selectOptions(screen.getByLabelText('District'), 'Huye')
    await screen.findByRole('option', { name: huyeCoop.name })
    await user.selectOptions(screen.getByLabelText('Cooperative'), huyeCoop.name)
    await waitFor(() =>
      expect(farmersApi.searchFarmers).toHaveBeenLastCalledWith(
        DEFAULT_PAGE,
        expect.objectContaining({ cooperativeId: 1 }),
      ),
    )
    expect(referenceApi.listCooperatives).toHaveBeenCalledWith(expect.anything(), 15)

    await user.click(screen.getByRole('button', { name: /Registered/ }))
    await waitFor(() =>
      expect(farmersApi.searchFarmers).toHaveBeenLastCalledWith(
        { ...DEFAULT_PAGE, sortBy: 'createdAt' },
        expect.anything(),
      ),
    )
  })

  it('shows an empty state that depends on whether a search is active', async () => {
    logInAs(USERS.registrar)
    vi.mocked(farmersApi.searchFarmers).mockResolvedValue(paged([]))
    renderRoute('/farmers?status=RETIRED')
    expect(await screen.findByText('No farmers match this search.')).toBeInTheDocument()
  })

  it('offers Register farmer only to the Registrar', async () => {
    logInAs(USERS.registrar)
    renderRoute('/farmers')
    expect(await screen.findByRole('link', { name: 'Register farmer' })).toHaveAttribute('href', '/farmers/new')
  })

  it.each([
    ['Finance Officer', USERS.finance],
    ['System Administrator', USERS.admin],
  ])('lets the %s search but not open a farmer (gap G6)', async (_, user) => {
    logInAs(user)
    renderRoute('/farmers')
    expect(await screen.findByText('Uwase Marie')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Uwase Marie' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Register farmer' })).not.toBeInTheDocument()
  })

  it('locks a Field Officer to their district and lists only its cooperatives', async () => {
    logInAs(USERS.officer)
    renderRoute('/farmers')
    expect(await screen.findByRole('link', { name: 'Uwase Marie' })).toBeInTheDocument()
    expect(screen.getByLabelText('District')).toBeDisabled()
    expect(await screen.findByRole('option', { name: huyeCoop.name })).toBeInTheDocument()
    expect(referenceApi.listCooperatives).toHaveBeenCalledWith(expect.anything(), 15)
    expect(screen.queryByRole('link', { name: 'Register farmer' })).not.toBeInTheDocument()
  })

  it('shows "Not available to you" to a farmer who types /farmers', async () => {
    logInAs(USERS.farmer)
    renderRoute('/farmers')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    expect(farmersApi.searchFarmers).not.toHaveBeenCalled()
  })
})
