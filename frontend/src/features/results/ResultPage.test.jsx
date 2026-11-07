import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as resultsApi from '../../api/results'
import { logInAs, USERS } from '../../test/farmers'
import { renderRoute } from '../../test/renderRoute'
import { correction, draftResult, line, pests, publishedResult, soil, submittedResult } from '../../test/results'

vi.mock('../../api/auth')
vi.mock('../../api/results')

const correctedResult = {
  ...publishedResult,
  scores: [line(soil, 15), line(pests, 9, { originalScore: 8, corrected: true })],
  finalScore: 84,
  grade: 'A',
  correctionCount: 1,
}

describe('ResultPage (FR-RES-04..06)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(resultsApi.listCorrections).mockResolvedValue([])
  })

  it('shows a farmer their published result with the correction history, read-only', async () => {
    logInAs(USERS.farmer)
    vi.mocked(resultsApi.getResult).mockResolvedValue(correctedResult)
    vi.mocked(resultsApi.listCorrections).mockResolvedValue([correction])
    renderRoute('/results/80')
    expect(await screen.findByRole('heading', { name: 'Maize, 2026B', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('84')).toBeInTheDocument()
    expect(screen.getByText(/corrected; originally/)).toBeInTheDocument()
    expect(await screen.findByRole('cell', { name: 'Re-inspection after the appeal' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '8 → 9' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: 'B → A' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← My results' })).toHaveAttribute('href', '/my/results')
    for (const name of ['Approve and publish', 'Return to draft', 'Correct a score']) {
      expect(screen.queryByRole('button', { name })).not.toBeInTheDocument()
    }
    expect(screen.queryByRole('link', { name: 'Edit scores' })).not.toBeInTheDocument()
  })

  it('shows "Not available to you" for a result the farmer may not see (403)', async () => {
    logInAs(USERS.farmer)
    vi.mocked(resultsApi.getResult).mockRejectedValue(
      new ApiError(403, 'You do not have permission to perform this action.'),
    )
    renderRoute('/results/81')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })

  it('lets the Agronomist approve a submitted result after a confirmation', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(resultsApi.getResult).mockResolvedValue(submittedResult)
    vi.mocked(resultsApi.approveResult).mockImplementation(async () => {
      vi.mocked(resultsApi.getResult).mockResolvedValue(publishedResult)
      return publishedResult
    })
    renderRoute('/results/80')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Approve and publish' }))
    const dialog = screen.getByRole('dialog', { name: 'Publish the result of Uwase Marie?' })
    await user.click(within(dialog).getByRole('button', { name: 'Approve and publish' }))
    expect(await screen.findByText('Result published: 78 (B)')).toBeInTheDocument()
    expect(resultsApi.approveResult).toHaveBeenCalledWith(80)
    expect(await screen.findByRole('button', { name: 'Correct a score' })).toBeInTheDocument()
  })

  it('makes the Agronomist give a reason to return a result', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(resultsApi.getResult).mockResolvedValue(submittedResult)
    vi.mocked(resultsApi.returnResult).mockResolvedValue(draftResult)
    renderRoute('/results/80')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Return to draft' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Return to draft' }))
    expect(await within(dialog).findByText('Give the reason.')).toBeInTheDocument()
    await user.type(within(dialog).getByLabelText('Reason'), 'Pest score looks too low')
    await user.click(within(dialog).getByRole('button', { name: 'Return to draft' }))
    expect(await screen.findByText('Result returned to draft')).toBeInTheDocument()
    expect(resultsApi.returnResult).toHaveBeenCalledWith(80, 'Pest score looks too low')
  })

  it('corrects one score of a published result, and shows the refusal of the same score', async () => {
    logInAs(USERS.agronomist)
    vi.mocked(resultsApi.getResult).mockResolvedValue(publishedResult)
    vi.mocked(resultsApi.correctResult).mockRejectedValueOnce(new ApiError(400, 'The score is already 8.'))
    vi.mocked(resultsApi.correctResult).mockResolvedValueOnce(correctedResult)
    renderRoute('/results/80')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Correct a score' }))
    const dialog = screen.getByRole('dialog', { name: 'Correct a published score' })
    await user.selectOptions(within(dialog).getByLabelText('Inspection'), 'Pest and disease control (now 8 of 10)')
    await user.type(within(dialog).getByLabelText('Corrected score'), '8')
    await user.type(within(dialog).getByLabelText('Reason'), 'Appeal')
    await user.click(within(dialog).getByRole('button', { name: 'Correct' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('The score is already 8.')

    const score = within(dialog).getByLabelText('Corrected score')
    await user.clear(score)
    await user.type(score, '9')
    await user.click(within(dialog).getByRole('button', { name: 'Correct' }))
    expect(await screen.findByText('Score corrected; final score now 84 (A)')).toBeInTheDocument()
    expect(resultsApi.correctResult).toHaveBeenLastCalledWith(80, {
      inspectionId: 102,
      correctedScore: 9,
      reason: 'Appeal',
    })
  })

  it('offers the Field Officer "Edit scores" on a draft only', async () => {
    logInAs(USERS.officer)
    vi.mocked(resultsApi.getResult).mockResolvedValue(draftResult)
    renderRoute('/results/80')
    expect(await screen.findByRole('link', { name: 'Edit scores' })).toHaveAttribute('href', '/enrolments/50/scores')
    expect(screen.queryByRole('button', { name: 'Approve and publish' })).not.toBeInTheDocument()
  })
})
