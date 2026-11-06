import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import { uwaseBeansCancelled, uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS } from '../../test/farmers'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/enrolments')

describe('EnrolmentPage (FR-ENR-06)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(enrolmentsApi.getEnrolment).mockResolvedValue(uwaseMaize)
  })

  it('shows a farmer their own enrolment, with Cancel', async () => {
    logInAs(USERS.farmer)
    renderRoute('/enrolments/50')
    expect(await screen.findByRole('heading', { name: 'Maize, 2026B', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('0.5 ha')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← My enrolments' })).toHaveAttribute('href', '/my/enrolments')
    expect(screen.getByRole('button', { name: 'Cancel enrolment' })).toBeInTheDocument()
    // Farmers do not get links into the staff pages.
    expect(screen.queryByRole('link', { name: 'Uwase Marie' })).not.toBeInTheDocument()
  })

  it('shows "Not available to you" for another farmer’s enrolment (403)', async () => {
    logInAs(USERS.farmer)
    vi.mocked(enrolmentsApi.getEnrolment).mockRejectedValue(
      new ApiError(403, 'You do not have permission to perform this action.'),
    )
    renderRoute('/enrolments/99')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })

  it('shows staff the cancellation details, read-only for an Agronomist', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(enrolmentsApi.getEnrolment).mockResolvedValue(uwaseBeansCancelled)
    renderRoute('/enrolments/51')
    expect(await screen.findByText('Planting maize only this season')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Uwase Marie' })).toHaveAttribute('href', '/farmers/7')
    expect(screen.getByRole('link', { name: '← Offering enrolments' })).toHaveAttribute(
      'href',
      '/offerings/13/enrolments',
    )
    expect(screen.queryByRole('button', { name: 'Cancel enrolment' })).not.toBeInTheDocument()
  })

  it('updates the page after the Registrar cancels with a reason', async () => {
    logInAs(USERS.registrar)
    const cancelled = {
      ...uwaseMaize,
      status: 'CANCELLED',
      cancelledAt: '2026-11-06T10:00:00Z',
      cancellationReason: 'Duplicate request',
    }
    vi.mocked(enrolmentsApi.cancelEnrolment).mockImplementation(async () => {
      vi.mocked(enrolmentsApi.getEnrolment).mockResolvedValue(cancelled) // what a reload returns from now on
      return cancelled
    })
    renderRoute('/enrolments/50')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Cancel enrolment' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText('Reason'), 'Duplicate request')
    await user.click(within(dialog).getByRole('button', { name: 'Cancel enrolment' }))
    expect(await screen.findByText('Duplicate request')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancel enrolment' })).not.toBeInTheDocument()
  })
})
