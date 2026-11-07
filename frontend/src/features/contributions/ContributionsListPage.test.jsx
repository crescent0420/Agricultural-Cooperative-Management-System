import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as contributionsApi from '../../api/contributions'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as seasonsApi from '../../api/seasons'
import { logInAs, USERS, uwaseSummary } from '../../test/farmers'
import { paged } from '../../test/paged'
import { contribution } from '../../test/payments'
import { renderRoute } from '../../test/renderRoute'
import { season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/contributions')
vi.mock('../../api/farmers')
vi.mock('../../api/seasons')

const DEFAULT_PAGE = { page: 0, size: 20, sortBy: 'createdAt', direction: 'desc' }

async function pickUwase(user, scope = screen) {
  await user.type(await scope.findByLabelText('Farmer'), 'Uwa')
  await user.click(await scope.findByRole('button', { name: /Uwase Marie/ }))
}

describe('ContributionsListPage (FR-FIN-01, FR-FIN-02)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(contributionsApi.listContributions).mockResolvedValue(paged([contribution]))
    vi.mocked(farmersApi.searchFarmers).mockResolvedValue(paged([uwaseSummary]))
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
  })

  it('shows every figure exactly as the backend sends it, with the two flags', async () => {
    logInAs(USERS.finance)
    renderRoute('/contributions')
    const row = (await screen.findByRole('link', { name: /Uwase Marie/ })).closest('tr')
    expect(within(row).getByText('50,000')).toBeInTheDocument()
    expect(within(row).getByText('30,000')).toBeInTheDocument()
    expect(within(row).getByText('40%')).toBeInTheDocument()
    expect(within(row).getByText('May enrol')).toBeInTheDocument()
    expect(within(row).getByText('Cannot collect yet')).toBeInTheDocument()
    expect(contributionsApi.listContributions).toHaveBeenCalledWith(DEFAULT_PAGE, {
      farmerId: undefined,
      seasonId: undefined,
    })
  })

  it('finds a farmer by name, then filters by that farmer (P1)', async () => {
    logInAs(USERS.registrar)
    renderRoute('/contributions')
    const user = userEvent.setup()
    await pickUwase(user)
    expect(farmersApi.searchFarmers).toHaveBeenCalledWith(expect.anything(), { name: 'Uwa' })
    await waitFor(() =>
      expect(contributionsApi.listContributions).toHaveBeenLastCalledWith(DEFAULT_PAGE, {
        farmerId: 7,
        seasonId: undefined,
      }),
    )
    // The chosen farmer replaces the search box, with a way to change it.
    expect(screen.getByRole('button', { name: 'Change farmer' })).toBeInTheDocument()
    expect(screen.queryByRole('searchbox')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'New contribution' })).not.toBeInTheDocument()
  })

  it('searches by membership number when one is typed', async () => {
    logInAs(USERS.finance)
    renderRoute('/contributions')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Farmer'), 'rcu-0000')
    await waitFor(() =>
      expect(farmersApi.searchFarmers).toHaveBeenCalledWith(expect.anything(), { membershipNumber: 'RCU-0000' }),
    )
  })

  it('lets the Finance Officer create a contribution, then opens it', async () => {
    logInAs(USERS.finance)
    vi.mocked(contributionsApi.createContribution).mockResolvedValue(contribution)
    vi.mocked(contributionsApi.getContribution).mockResolvedValue(contribution)
    const { router } = renderRoute('/contributions')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'New contribution' }))
    const dialog = screen.getByRole('dialog', { name: 'New contribution' })
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))
    expect(await within(dialog).findByText('Find and choose the farmer.')).toBeInTheDocument()

    await pickUwase(user, within(dialog))
    await within(dialog).findByRole('option', { name: '2026B' })
    await user.selectOptions(within(dialog).getByLabelText('Season'), '2026B')
    await user.type(within(dialog).getByLabelText('Amount due'), '50000')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))
    expect(await screen.findByText('Contribution of Uwase Marie for 2026B created')).toBeInTheDocument()
    expect(contributionsApi.createContribution).toHaveBeenCalledWith({ farmerId: 7, seasonId: 3, amountDue: 50000 })
    await waitFor(() => expect(router.state.location.pathname).toBe('/contributions/70'))
  })

  it('shows the backend refusal of a second contribution for the same season (409)', async () => {
    logInAs(USERS.finance)
    vi.mocked(contributionsApi.createContribution).mockRejectedValue(
      new ApiError(409, "Farmer RCU-000007 already has a contribution for season '2026B'."),
    )
    renderRoute('/contributions')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'New contribution' }))
    const dialog = screen.getByRole('dialog')
    await pickUwase(user, within(dialog))
    await within(dialog).findByRole('option', { name: '2026B' })
    await user.selectOptions(within(dialog).getByLabelText('Season'), '2026B')
    await user.type(within(dialog).getByLabelText('Amount due'), '50000')
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('already has a contribution')
  })

  it('is not available to a farmer', async () => {
    logInAs(USERS.farmer)
    renderRoute('/contributions')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
