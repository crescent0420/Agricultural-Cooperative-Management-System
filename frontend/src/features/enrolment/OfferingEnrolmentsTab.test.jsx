import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import * as offeringsApi from '../../api/offerings'
import { uwaseBeansCancelled, uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { maizeOffering } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/enrolments')
vi.mock('../../api/offerings')

describe('Offering page, Enrolments tab (FR-ENR-05)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(offeringsApi.getOffering).mockResolvedValue(maizeOffering)
    vi.mocked(enrolmentsApi.listOfferingEnrolments).mockResolvedValue(paged([uwaseMaize]))
  })

  it('lists the enrolled farmers, with links to the farmer and the enrolment', async () => {
    logInAs(USERS.officer)
    renderRoute('/offerings/12/enrolments')
    expect(await screen.findByRole('link', { name: 'Uwase Marie' })).toHaveAttribute('href', '/farmers/7')
    expect(screen.getByRole('link', { name: 'RCU-000007' })).toHaveAttribute('href', '/enrolments/50')
    expect(enrolmentsApi.listOfferingEnrolments).toHaveBeenCalledWith(
      12,
      { page: 0, size: 20, sortBy: 'enrolledAt', direction: 'asc' },
      'ENROLLED',
    )
    const tabs = within(screen.getByRole('navigation', { name: 'Offering' })).getAllByRole('link')
    expect(tabs.map((t) => t.textContent)).toEqual(['Overview', 'Enrolments', 'Inspections', 'Results'])
    // The offering's own Field Officer enters scores from here (FR-RES-01).
    expect(screen.getByRole('link', { name: 'Scores of Uwase Marie' })).toHaveAttribute('href', '/enrolments/50/scores')
    // Only the Registrar cancels from here.
    expect(screen.queryByRole('button', { name: /Cancel the enrolment/ })).not.toBeInTheDocument()
  })

  it('shows cancelled enrolments with their reason', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(enrolmentsApi.listOfferingEnrolments).mockResolvedValue(paged([uwaseBeansCancelled]))
    renderRoute('/offerings/12/enrolments?status=CANCELLED')
    expect(await screen.findByRole('cell', { name: 'Planting maize only this season' })).toBeInTheDocument()
    expect(enrolmentsApi.listOfferingEnrolments).toHaveBeenCalledWith(12, expect.anything(), 'CANCELLED')
  })

  it('makes the Registrar give a reason before cancelling (E1)', async () => {
    logInAs(USERS.registrar)
    vi.mocked(enrolmentsApi.cancelEnrolment).mockResolvedValue({ ...uwaseMaize, status: 'CANCELLED' })
    renderRoute('/offerings/12/enrolments')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Cancel the enrolment of Uwase Marie' }))
    const dialog = screen.getByRole('dialog', { name: 'Cancel this enrolment?' })
    await user.click(within(dialog).getByRole('button', { name: 'Cancel enrolment' }))
    expect(await within(dialog).findByText('Give the reason for cancelling.')).toBeInTheDocument()
    expect(enrolmentsApi.cancelEnrolment).not.toHaveBeenCalled()

    await user.type(within(dialog).getByLabelText('Reason'), ' Duplicate request ')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel enrolment' }))
    expect(await screen.findByText('Enrolment in Maize (2026B) cancelled')).toBeInTheDocument()
    expect(enrolmentsApi.cancelEnrolment).toHaveBeenCalledWith(50, 'Duplicate request')
    await waitFor(() => expect(vi.mocked(offeringsApi.getOffering).mock.calls.length).toBeGreaterThan(1))
  })

  it('shows "Not available to you" to a Field Officer of another offering (403, E3)', async () => {
    logInAs(USERS.officer)
    vi.mocked(enrolmentsApi.listOfferingEnrolments).mockRejectedValue(
      new ApiError(403, 'You do not have permission to perform this action.'),
    )
    renderRoute('/offerings/12/enrolments')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    // The offering itself stays visible above the tab.
    expect(screen.getByRole('heading', { name: 'Maize, 2026B', level: 1 })).toBeInTheDocument()
  })
})
