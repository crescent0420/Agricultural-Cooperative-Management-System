import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as offeringsApi from '../../api/offerings'
import * as referenceApi from '../../api/reference'
import * as seasonsApi from '../../api/seasons'
import { huye, logInAs, musanze, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { fullOffering, maize, maizeOffering, season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/offerings')
vi.mock('../../api/reference')
vi.mock('../../api/seasons')

const DEFAULT_PAGE = { page: 0, size: 20, sortBy: 'createdAt', direction: 'desc' }

describe('Offerings lists (FR-OFF-01, FR-OFF-02)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(referenceApi.listProgrammes).mockResolvedValue(paged([maize]))
    vi.mocked(offeringsApi.listOfferings).mockResolvedValue(paged([maizeOffering, fullOffering]))
    vi.mocked(offeringsApi.listMyOfferings).mockResolvedValue(paged([maizeOffering]))
  })

  it('shows places, a Full badge, and sends the filters and sorting to the API', async () => {
    logInAs(USERS.registrar)
    renderRoute('/offerings')
    expect(await screen.findByRole('link', { name: 'Maize' })).toHaveAttribute('href', '/offerings/12')
    expect(screen.getByText('12 / 40')).toBeInTheDocument()
    const beansRow = screen.getByRole('link', { name: 'Beans' }).closest('tr')
    expect(within(beansRow).getByText('Full')).toBeInTheDocument()
    expect(screen.getAllByText('Full')).toHaveLength(1)
    expect(offeringsApi.listOfferings).toHaveBeenCalledWith(DEFAULT_PAGE, {
      seasonId: undefined,
      districtId: undefined,
      programmeId: undefined,
    })

    const user = userEvent.setup()
    await screen.findByRole('option', { name: '2026B' })
    await user.selectOptions(screen.getByLabelText('Season'), '2026B')
    await screen.findByRole('option', { name: 'Huye' })
    await user.selectOptions(screen.getByLabelText('District'), 'Huye')
    await screen.findByRole('option', { name: 'Maize' })
    await user.selectOptions(screen.getByLabelText('Programme'), 'Maize')
    await waitFor(() =>
      expect(offeringsApi.listOfferings).toHaveBeenLastCalledWith(DEFAULT_PAGE, {
        seasonId: 3,
        districtId: 15,
        programmeId: 30,
      }),
    )

    await user.click(screen.getByRole('button', { name: /Enrolled \/ max/ }))
    await waitFor(() =>
      expect(offeringsApi.listOfferings).toHaveBeenLastCalledWith(
        { ...DEFAULT_PAGE, sortBy: 'enrolledCount', direction: 'asc' },
        expect.anything(),
      ),
    )
    expect(screen.getByRole('link', { name: 'New offering' })).toHaveAttribute('href', '/offerings/new')
  })

  it('locks an Agronomist to their district, without New offering', async () => {
    logInAs(USERS.agronomist)
    renderRoute('/offerings')
    expect(await screen.findByRole('link', { name: 'Maize' })).toBeInTheDocument()
    expect(screen.getByLabelText('District')).toBeDisabled()
    expect(offeringsApi.listOfferings).toHaveBeenCalledWith(DEFAULT_PAGE, expect.objectContaining({ districtId: 15 }))
    expect(await screen.findByRole('option', { name: 'Maize' })).toBeInTheDocument()
    expect(referenceApi.listProgrammes).toHaveBeenCalledWith(expect.anything(), 15)
    expect(screen.queryByRole('link', { name: 'New offering' })).not.toBeInTheDocument()
  })

  it('shows "Not available to you" to a farmer who types /offerings', async () => {
    logInAs(USERS.farmer)
    renderRoute('/offerings')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    expect(offeringsApi.listOfferings).not.toHaveBeenCalled()
  })

  it('lists a Field Officer’s own offerings, by season', async () => {
    logInAs(USERS.officer)
    renderRoute('/offerings/mine')
    expect(await screen.findByRole('link', { name: 'Maize' })).toBeInTheDocument()
    expect(offeringsApi.listMyOfferings).toHaveBeenCalledWith(DEFAULT_PAGE, undefined)
    const user = userEvent.setup()
    await screen.findByRole('option', { name: '2027A' })
    await user.selectOptions(screen.getByLabelText('Season'), '2027A')
    await waitFor(() => expect(offeringsApi.listMyOfferings).toHaveBeenLastCalledWith(DEFAULT_PAGE, 4))
  })

  it('keeps "My assigned offerings" for Field Officers only', async () => {
    logInAs(USERS.registrar)
    renderRoute('/offerings/mine')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    expect(offeringsApi.listMyOfferings).not.toHaveBeenCalled()
  })
})
