import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as seasonsApi from '../../api/seasons'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { openWindowRow, season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/seasons')

/** A season with no window yet (editable), or one whose window is open. */
function given(season, windows) {
  vi.mocked(seasonsApi.getSeason).mockResolvedValue(season)
  vi.mocked(seasonsApi.listWindows).mockResolvedValue(paged(windows))
}

describe('SeasonPage (FR-SEA-02..04)', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows the season and its window history, newest first', async () => {
    given(season2026B, [openWindowRow])
    logInAs(USERS.officer)
    renderRoute('/seasons/3')
    expect(await screen.findByRole('heading', { name: '2026B', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('2 ha')).toBeInTheDocument()
    expect(await screen.findByRole('cell', { name: 'demo.registrar' })).toBeInTheDocument()
    expect(seasonsApi.listWindows).toHaveBeenCalledWith(3, { page: 0, size: 20, sortBy: 'openedAt', direction: 'desc' })
    // Read-only for everyone but the Registrar.
    expect(screen.queryByRole('button', { name: /enrolment window/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('opens the enrolment window after a confirmation', async () => {
    given(season2027A, [])
    vi.mocked(seasonsApi.openWindow).mockResolvedValue({ ...openWindowRow, seasonId: 4, seasonName: '2027A' })
    logInAs(USERS.registrar)
    renderRoute('/seasons/4')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Open enrolment window' }))
    const dialog = screen.getByRole('dialog', { name: 'Open enrolment for 2027A?' })
    expect(dialog).toHaveTextContent('can no longer be edited')
    await user.click(within(dialog).getByRole('button', { name: 'Open window' }))

    expect(await screen.findByText('Enrolment window opened')).toBeInTheDocument()
    expect(seasonsApi.openWindow).toHaveBeenCalledWith(4)
  })

  it('shows the backend refusal for an ended season (422) in the dialog', async () => {
    given(season2027A, [])
    vi.mocked(seasonsApi.openWindow).mockRejectedValue(
      new ApiError(422, 'This season ended on 2027-07-31; an enrolment window can no longer be opened.'),
    )
    logInAs(USERS.registrar)
    renderRoute('/seasons/4')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Open enrolment window' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Open window' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('can no longer be opened')
  })

  it('offers Close for an open window, and closes it', async () => {
    given(season2026B, [openWindowRow])
    vi.mocked(seasonsApi.closeWindow).mockResolvedValue({ ...openWindowRow, status: 'CLOSED' })
    logInAs(USERS.registrar)
    renderRoute('/seasons/3')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Close enrolment window' }))
    expect(screen.queryByRole('button', { name: 'Open enrolment window' })).not.toBeInTheDocument()
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Close window' }))
    expect(await screen.findByText('Enrolment window closed')).toBeInTheDocument()
    expect(seasonsApi.closeWindow).toHaveBeenCalledWith(3)
  })

  it('offers no Edit once a window has been opened, and says why (S1)', async () => {
    given({ ...season2026B, enrolmentOpen: false }, [{ ...openWindowRow, status: 'CLOSED' }])
    logInAs(USERS.registrar)
    renderRoute('/seasons/3')
    expect(await screen.findByText(/can no longer be edited/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Open enrolment window' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })

  it('lets the Registrar edit a season that has no window yet', async () => {
    given(season2027A, [])
    vi.mocked(seasonsApi.updateSeason).mockResolvedValue({ ...season2027A, maxLandHectares: 3 })
    logInAs(USERS.registrar)
    renderRoute('/seasons/4')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Edit' }))
    const dialog = screen.getByRole('dialog', { name: 'Edit 2027A' })
    const land = within(dialog).getByLabelText('Maximum land per farmer (ha)')
    await user.clear(land)
    await user.type(land, '3')
    vi.mocked(seasonsApi.getSeason).mockResolvedValue({ ...season2027A, maxLandHectares: 3 }) // what a reload returns
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Season saved')).toBeInTheDocument()
    expect(seasonsApi.updateSeason).toHaveBeenCalledWith(
      4,
      expect.objectContaining({ name: '2027A', maxLandHectares: 3 }),
    )
    expect(await screen.findByText('3 ha')).toBeInTheDocument()
  })
})
