import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as offeringsApi from '../../api/offerings'
import * as resultsApi from '../../api/results'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { draftResult, publishedResult, submittedResult, summaryOf } from '../../test/results'
import { maizeOffering } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/offerings')
vi.mock('../../api/results')

describe('Result lists (FR-RES-03, FR-RES-07)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(offeringsApi.getOffering).mockResolvedValue(maizeOffering)
  })

  it('lists an offering’s results with status badges, and filters Submitted for the Agronomist (R3)', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(resultsApi.listOfferingResults).mockResolvedValue(
      paged([summaryOf(draftResult), summaryOf({ ...submittedResult, id: 81, farmerName: 'Habimana Jean' })]),
    )
    renderRoute('/offerings/12/results')
    expect(await screen.findByRole('link', { name: 'Habimana Jean' })).toHaveAttribute('href', '/results/81')
    const table = screen.getByRole('table')
    expect(within(table).getByText('Draft')).toBeInTheDocument()
    expect(within(table).getByText('Submitted')).toBeInTheDocument()
    expect(resultsApi.listOfferingResults).toHaveBeenCalledWith(
      12,
      { page: 0, size: 20, sortBy: 'id', direction: 'asc' },
      '',
    )

    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText('Status'), 'Submitted')
    await waitFor(() =>
      expect(resultsApi.listOfferingResults).toHaveBeenLastCalledWith(12, expect.anything(), 'SUBMITTED'),
    )
    await user.click(screen.getByRole('button', { name: /Final score/ }))
    await waitFor(() =>
      expect(resultsApi.listOfferingResults).toHaveBeenLastCalledWith(
        12,
        expect.objectContaining({ sortBy: 'finalScore' }),
        'SUBMITTED',
      ),
    )
  })

  it('lists the farmer’s own published results', async () => {
    logInAs(USERS.farmer)
    vi.mocked(resultsApi.listMyResults).mockResolvedValue(paged([summaryOf(publishedResult)]))
    renderRoute('/my/results')
    expect(await screen.findByRole('link', { name: 'Maize' })).toHaveAttribute('href', '/results/80')
    expect(screen.getByRole('cell', { name: 'B' })).toBeInTheDocument()
    expect(resultsApi.listMyResults).toHaveBeenCalledWith({ page: 0, size: 20, sortBy: 'id', direction: 'desc' })
  })

  it('keeps My results for farmers', async () => {
    logInAs(USERS.registrar)
    renderRoute('/my/results')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
