import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import * as seasonsApi from '../../api/seasons'
import { uwaseBeansCancelled, uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/enrolments')
vi.mock('../../api/seasons')

describe('MyEnrolmentsPage (FR-ENR-03, FR-ENR-04)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.farmer)
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
    vi.mocked(enrolmentsApi.listMyEnrolments).mockResolvedValue(paged([uwaseMaize, uwaseBeansCancelled]))
  })

  it('lists the farmer’s enrolments, marking cancelled ones with their reason', async () => {
    renderRoute('/my/enrolments')
    expect(await screen.findByRole('link', { name: 'Maize' })).toHaveAttribute('href', '/enrolments/50')
    expect(screen.getByText('Reason: Planting maize only this season')).toBeInTheDocument()
    const beansRow = screen.getByRole('link', { name: 'Beans' }).closest('tr')
    expect(within(beansRow).getByText('Cancelled')).toBeInTheDocument()
    expect(within(beansRow).queryByRole('button', { name: /Cancel/ })).not.toBeInTheDocument()
    expect(enrolmentsApi.listMyEnrolments).toHaveBeenCalledWith(
      { page: 0, size: 20, sortBy: 'enrolledAt', direction: 'desc' },
      { seasonId: undefined, status: '' },
    )
  })

  it('filters by season and status', async () => {
    renderRoute('/my/enrolments')
    const user = userEvent.setup()
    await screen.findByRole('option', { name: '2026B' })
    await user.selectOptions(screen.getByLabelText('Season'), '2026B')
    await user.selectOptions(screen.getByLabelText('Status'), 'Cancelled')
    await waitFor(() =>
      expect(enrolmentsApi.listMyEnrolments).toHaveBeenLastCalledWith(expect.anything(), {
        seasonId: 3,
        status: 'CANCELLED',
      }),
    )
  })

  it('lets the farmer cancel without a reason (it is optional for farmers)', async () => {
    vi.mocked(enrolmentsApi.cancelEnrolment).mockResolvedValue({ ...uwaseMaize, status: 'CANCELLED' })
    renderRoute('/my/enrolments')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Cancel Maize' }))
    const dialog = screen.getByRole('dialog', { name: 'Cancel this enrolment?' })
    expect(within(dialog).getByLabelText('Reason (optional)')).toBeInTheDocument()
    await user.click(within(dialog).getByRole('button', { name: 'Cancel enrolment' }))
    expect(await screen.findByText('Enrolment in Maize (2026B) cancelled')).toBeInTheDocument()
    expect(enrolmentsApi.cancelEnrolment).toHaveBeenCalledWith(50, '')
  })

  it('shows the backend refusal once the window has closed (422)', async () => {
    vi.mocked(enrolmentsApi.cancelEnrolment).mockRejectedValue(
      new ApiError(422, 'The enrolment window is closed; ask the Union Registrar to cancel.'),
    )
    renderRoute('/my/enrolments')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Cancel Maize' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel enrolment' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('ask the Union Registrar to cancel')
  })
})
