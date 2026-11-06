import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as seasonsApi from '../../api/seasons'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/seasons')

async function openNewSeason(user) {
  await user.click(await screen.findByRole('button', { name: 'New season' }))
  return screen.getByRole('dialog', { name: 'New season' })
}

async function fill(user, dialog, values) {
  for (const [label, value] of Object.entries(values)) {
    const field = within(dialog).getByLabelText(label)
    await user.clear(field)
    if (value !== '') await user.type(field, value)
  }
}

describe('SeasonsListPage (FR-SEA-01, FR-SEA-02)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
  })

  it('lists the seasons, newest first, with their enrolment status', async () => {
    logInAs(USERS.finance)
    renderRoute('/seasons')
    expect(await screen.findByRole('link', { name: '2027A' })).toHaveAttribute('href', '/seasons/4')
    expect(screen.getByText('Enrolment open')).toBeInTheDocument()
    expect(screen.getByText('Enrolment closed')).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '1 Feb 2027' })).toBeInTheDocument()
    expect(seasonsApi.listSeasons).toHaveBeenCalledWith({ page: 0, size: 20, sortBy: 'startsOn', direction: 'desc' })
    expect(screen.queryByRole('button', { name: 'New season' })).not.toBeInTheDocument()
  })

  it('lets the Registrar create a season', async () => {
    logInAs(USERS.registrar)
    vi.mocked(seasonsApi.createSeason).mockResolvedValue({ ...season2027A, id: 5, name: '2027B' })
    renderRoute('/seasons')
    const user = userEvent.setup()
    const dialog = await openNewSeason(user)
    await fill(user, dialog, {
      Name: ' 2027B ',
      'Starts on': '2027-09-01',
      'Ends on': '2028-01-31',
      'Maximum land per farmer (ha)': '2.5',
      'Distribution date': '2027-09-15',
    })
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await screen.findByText('Season 2027B created')).toBeInTheDocument()
    expect(seasonsApi.createSeason).toHaveBeenCalledWith({
      name: '2027B',
      startsOn: '2027-09-01',
      endsOn: '2028-01-31',
      maxLandHectares: 2.5,
      distributionDate: '2027-09-15',
    })
  })

  it('checks the dates and the land limit like the backend before calling it', async () => {
    logInAs(USERS.registrar)
    renderRoute('/seasons')
    const user = userEvent.setup()
    const dialog = await openNewSeason(user)
    await fill(user, dialog, {
      Name: '2027B',
      'Starts on': '2027-09-01',
      'Ends on': '2027-08-01',
      'Maximum land per farmer (ha)': '2.555',
      'Distribution date': '2028-02-01',
    })
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await within(dialog).findByText('The end must be after the start.')).toBeInTheDocument()
    expect(within(dialog).getByText('Enter more than 0, with at most 2 decimals.')).toBeInTheDocument()
    expect(within(dialog).getByText('The distribution date must be inside the season.')).toBeInTheDocument()
    expect(seasonsApi.createSeason).not.toHaveBeenCalled()
  })

  it('keeps the dialog open with the backend message for a duplicate name (409)', async () => {
    logInAs(USERS.registrar)
    vi.mocked(seasonsApi.createSeason).mockRejectedValue(new ApiError(409, "Season '2027A' already exists."))
    renderRoute('/seasons')
    const user = userEvent.setup()
    const dialog = await openNewSeason(user)
    await fill(user, dialog, {
      Name: '2027A',
      'Starts on': '2027-02-01',
      'Ends on': '2027-07-31',
      'Maximum land per farmer (ha)': '2',
      'Distribution date': '2027-02-15',
    })
    await user.click(within(dialog).getByRole('button', { name: 'Create' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent("Season '2027A' already exists.")
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument())
  })
})
