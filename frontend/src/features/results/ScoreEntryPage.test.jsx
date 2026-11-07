import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as inspectionsApi from '../../api/inspections'
import * as resultsApi from '../../api/results'
import { uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { completeList, draftResult, line, pests, soil, submittedResult, summaryOf } from '../../test/results'

vi.mock('../../api/auth')
vi.mock('../../api/enrolments')
vi.mock('../../api/farmers')
vi.mock('../../api/inspections')
vi.mock('../../api/results')

describe('ScoreEntryPage (FR-RES-01, FR-RES-02)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.officer)
    vi.mocked(enrolmentsApi.getEnrolment).mockResolvedValue(uwaseMaize)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue(completeList)
    vi.mocked(farmersApi.listFarmerResults).mockResolvedValue(paged([summaryOf(draftResult)]))
    vi.mocked(resultsApi.getResult).mockResolvedValue(draftResult)
  })

  it('finds the enrolment’s draft and fills in its scores (R1)', async () => {
    renderRoute('/enrolments/50/scores')
    expect(await screen.findByLabelText('Soil and land preparation')).toHaveValue(15)
    expect(screen.getByLabelText('Pest and disease control')).toHaveValue(null)
    expect(screen.getByText('0 to 20 · weight 40%')).toBeInTheDocument()
    expect(farmersApi.listFarmerResults).toHaveBeenCalledWith(7, expect.anything())
    expect(resultsApi.getResult).toHaveBeenCalledWith(80)
  })

  it('starts empty when no score has been entered yet', async () => {
    vi.mocked(farmersApi.listFarmerResults).mockResolvedValue(paged([]))
    renderRoute('/enrolments/50/scores')
    expect(await screen.findByLabelText('Soil and land preparation')).toHaveValue(null)
    expect(resultsApi.getResult).not.toHaveBeenCalled()
  })

  it('refuses a score above the inspection’s maximum before calling the API', async () => {
    renderRoute('/enrolments/50/scores')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Pest and disease control'), '11')
    await user.click(screen.getByRole('button', { name: 'Save draft' }))
    expect(await screen.findByText('The score must be between 0 and 10.')).toBeInTheDocument()
    expect(resultsApi.enterScores).not.toHaveBeenCalled()
  })

  it('saves the draft', async () => {
    vi.mocked(resultsApi.enterScores).mockResolvedValue({ ...draftResult, scores: [line(soil, 15), line(pests, 8)] })
    renderRoute('/enrolments/50/scores')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Pest and disease control'), '8')
    await user.click(screen.getByRole('button', { name: 'Save draft' }))
    expect(await screen.findByText('Scores saved as a draft')).toBeInTheDocument()
    expect(resultsApi.enterScores).toHaveBeenCalledWith(50, [
      { inspectionId: 101, score: 15 },
      { inspectionId: 102, score: 8 },
    ])
  })

  it('saves and submits after a confirmation', async () => {
    vi.mocked(resultsApi.enterScores).mockResolvedValue({ ...draftResult, scores: [line(soil, 15), line(pests, 8)] })
    vi.mocked(resultsApi.submitResult).mockResolvedValue(submittedResult)
    renderRoute('/enrolments/50/scores')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Pest and disease control'), '8')
    await user.click(screen.getByRole('button', { name: 'Submit for approval' }))
    const dialog = screen.getByRole('dialog', { name: 'Submit the result of Uwase Marie?' })
    vi.mocked(resultsApi.getResult).mockResolvedValue(submittedResult) // what a reload returns
    await user.click(within(dialog).getByRole('button', { name: 'Submit' }))
    expect(await screen.findByText('Result submitted for approval: 78 (B)')).toBeInTheDocument()
    expect(resultsApi.enterScores).toHaveBeenCalled()
    expect(resultsApi.submitResult).toHaveBeenCalledWith(80)
  })

  it('shows the backend refusal for a missing score (422) in the dialog', async () => {
    vi.mocked(resultsApi.submitResult).mockRejectedValue(
      new ApiError(422, 'Scores are missing for: Pest and disease control.'),
    )
    renderRoute('/enrolments/50/scores')
    const user = userEvent.setup()
    await screen.findByLabelText('Soil and land preparation')
    await user.click(screen.getByRole('button', { name: 'Submit for approval' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Submit' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(
      'Scores are missing for: Pest and disease control.',
    )
    expect(resultsApi.enterScores).not.toHaveBeenCalled() // nothing changed, so only the submit was sent
  })

  it('says, when a change is tried, that a submitted result can no longer change', async () => {
    vi.mocked(farmersApi.listFarmerResults).mockResolvedValue(paged([summaryOf(submittedResult)]))
    vi.mocked(resultsApi.getResult).mockResolvedValue(submittedResult)
    renderRoute('/enrolments/50/scores')
    const user = userEvent.setup()
    const field = await screen.findByLabelText('Pest and disease control')
    expect(screen.queryByText(/can be changed only while it is a draft/)).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Save draft' }))
    expect(screen.getByRole('alert')).toHaveTextContent('can be changed only while it is a draft')
    await user.type(field, '1')
    expect(field).toHaveValue(8)
    await waitFor(() => expect(resultsApi.enterScores).not.toHaveBeenCalled())
  })
})

describe('ScoreEntryPage, first submit', () => {
  it('keeps the dialog and shows the refusal when the first save creates the draft (regression)', async () => {
    vi.resetAllMocks()
    logInAs(USERS.officer)
    vi.mocked(enrolmentsApi.getEnrolment).mockResolvedValue(uwaseMaize)
    vi.mocked(inspectionsApi.listInspections).mockResolvedValue(completeList)
    vi.mocked(farmersApi.listFarmerResults).mockResolvedValue(paged([]))
    vi.mocked(resultsApi.getResult).mockResolvedValue(draftResult)
    vi.mocked(resultsApi.enterScores).mockImplementation(async () => {
      // From now on the farmer has a result: the page's reload finds it.
      vi.mocked(farmersApi.listFarmerResults).mockResolvedValue(paged([summaryOf(draftResult)]))
      return draftResult
    })
    vi.mocked(resultsApi.submitResult).mockRejectedValue(
      new ApiError(422, 'Scores are missing for: Pest and disease control.'),
    )
    renderRoute('/enrolments/50/scores')
    const user = userEvent.setup()
    await user.type(await screen.findByLabelText('Soil and land preparation'), '15')
    await user.click(screen.getByRole('button', { name: 'Submit for approval' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Submit' }))
    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('Scores are missing')
    await waitFor(() => expect(resultsApi.getResult).toHaveBeenCalled())
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })
})
