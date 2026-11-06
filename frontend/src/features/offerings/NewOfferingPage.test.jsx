import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as offeringsApi from '../../api/offerings'
import * as referenceApi from '../../api/reference'
import * as seasonsApi from '../../api/seasons'
import { huye, logInAs, musanze, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import {
  huyeOfficer,
  huyeOfficer2,
  maize,
  maizeOffering,
  musanzeOfficer,
  potatoes,
  season2026B,
  season2027A,
} from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/offerings')
vi.mock('../../api/reference')
vi.mock('../../api/seasons')

async function choose(user, label, option) {
  await screen.findByRole('option', { name: option })
  await user.selectOptions(screen.getByLabelText(label), option)
}

async function fillForm(user) {
  await choose(user, 'District', 'Huye')
  await choose(user, 'Crop programme', 'Maize')
  await choose(user, 'Season', '2026B')
  await choose(user, 'Field Officer', 'huye.officer2')
  await user.type(screen.getByLabelText('Hectares per participant'), '0.5')
  await user.type(screen.getByLabelText('Maximum participants'), '40')
}

describe('NewOfferingPage (FR-OFF-03, G8)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.registrar)
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(referenceApi.listProgrammes).mockImplementation(async (_, districtId) =>
      paged(districtId === 15 ? [maize] : [potatoes]),
    )
    vi.mocked(offeringsApi.listFieldOfficers).mockImplementation(async (districtId) =>
      districtId === 15 ? [huyeOfficer, huyeOfficer2] : [musanzeOfficer],
    )
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
    vi.mocked(offeringsApi.getOffering).mockResolvedValue(maizeOffering)
  })

  it('checks every field before calling the API', async () => {
    renderRoute('/offerings/new')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Hectares per participant'), '0.555')
    await user.type(screen.getByLabelText('Maximum participants'), '0')
    await user.click(screen.getByRole('button', { name: 'Create offering' }))

    expect(await screen.findByText('Choose a district.')).toBeInTheDocument()
    expect(screen.getByText('Choose a crop programme.')).toBeInTheDocument()
    expect(screen.getByText('Choose a season.')).toBeInTheDocument()
    expect(screen.getByText('Choose a Field Officer.')).toBeInTheDocument()
    expect(screen.getByText(/Enter at least 0.01/)).toBeInTheDocument()
    expect(screen.getByText('Enter a whole number from 1 to 100,000.')).toBeInTheDocument()
    expect(offeringsApi.createOffering).not.toHaveBeenCalled()
  })

  it('loads the programmes and Field Officers of the chosen district, and reloads them when it changes', async () => {
    renderRoute('/offerings/new')
    const user = userEvent.setup()
    expect(await screen.findByLabelText('Field Officer')).toBeDisabled()
    await choose(user, 'District', 'Huye')
    expect(await screen.findByRole('option', { name: 'huye.officer2' })).toBeInTheDocument()
    expect(offeringsApi.listFieldOfficers).toHaveBeenCalledWith(15)

    await choose(user, 'District', 'Musanze')
    expect(await screen.findByRole('option', { name: 'demo.officer.musanze' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'huye.officer2' })).not.toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Irish potatoes' })).toBeInTheDocument()
    expect(offeringsApi.listFieldOfficers).toHaveBeenLastCalledWith(21)
  })

  it('shows the season’s land limit as a hint, and says when a district has no Field Officer', async () => {
    vi.mocked(offeringsApi.listFieldOfficers).mockResolvedValue([])
    renderRoute('/offerings/new')
    const user = userEvent.setup()
    await choose(user, 'Season', '2026B')
    expect(screen.getByText('2026B allows at most 2 ha per farmer.')).toBeInTheDocument()
    await choose(user, 'District', 'Huye')
    expect(await screen.findByText(/has no enabled Field Officer/)).toBeInTheDocument()
  })

  it('creates the offering and opens it', async () => {
    vi.mocked(offeringsApi.createOffering).mockResolvedValue(maizeOffering)
    const { router } = renderRoute('/offerings/new')
    const user = userEvent.setup()
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Create offering' }))

    expect(await screen.findByText('Maize offered in 2026B')).toBeInTheDocument()
    expect(offeringsApi.createOffering).toHaveBeenCalledWith({
      programmeId: 30,
      seasonId: 3,
      fieldOfficerId: 8,
      hectaresPerParticipant: 0.5,
      maxParticipants: 40,
    })
    await waitFor(() => expect(router.state.location.pathname).toBe('/offerings/12'))
  })

  it.each([
    [409, "Programme 'Maize' is already offered in season '2026B'."],
    [422, "hectaresPerParticipant (3) is larger than the season's land limit per farmer (2)."],
  ])('shows the backend refusal (%i) and stays on the form', async (status, message) => {
    vi.mocked(offeringsApi.createOffering).mockRejectedValue(new ApiError(status, message))
    const { router } = renderRoute('/offerings/new')
    const user = userEvent.setup()
    await fillForm(user)
    await user.click(screen.getByRole('button', { name: 'Create offering' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(router.state.location.pathname).toBe('/offerings/new')
  })
})
