import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as contributionsApi from '../../api/contributions'
import { ApiError, SERVER_UNREACHABLE } from '../../api/errors'
import * as paymentsApi from '../../api/payments'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { adjustment, confirmedPayment, contribution, pendingPayment } from '../../test/payments'
import { renderRoute } from '../../test/renderRoute'

vi.mock('../../api/auth')
vi.mock('../../api/contributions')
vi.mock('../../api/payments', async (importOriginal) => ({
  ...(await importOriginal()),
  recordPayment: vi.fn(),
  confirmPayment: vi.fn(),
  failPayment: vi.fn(),
}))

const REFERENCE = /^ACMS-70-\d{8}T\d{6}-[0-9a-f]{8}$/

async function openRecord(user) {
  await user.click(await screen.findByRole('button', { name: 'Record payment' }))
  const dialog = screen.getByRole('dialog', { name: 'Record a payment' })
  await user.type(within(dialog).getByLabelText('Amount'), '15000')
  await user.selectOptions(within(dialog).getByLabelText('Method'), 'Mobile money')
  return dialog
}

describe('ContributionPage (FR-FIN-03..06)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(contributionsApi.getContribution).mockResolvedValue(contribution)
    vi.mocked(contributionsApi.listContributionPayments).mockResolvedValue(paged([pendingPayment, confirmedPayment]))
    vi.mocked(contributionsApi.listAdjustments).mockResolvedValue(paged([adjustment]))
  })

  it('shows the balance, the payments and the adjustment history; Confirm only on pending payments', async () => {
    logInAs(USERS.finance)
    renderRoute('/contributions/70')
    const balance = await screen.findByRole('region', { name: 'Balance' })
    expect(within(balance).getByText('30,000')).toBeInTheDocument()
    expect(within(balance).getByText('40%')).toBeInTheDocument()
    expect(within(balance).getByText('May enrol')).toBeInTheDocument()
    expect(
      await screen.findByRole('button', { name: `Confirm ${pendingPayment.transactionReference}` }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: `Confirm ${confirmedPayment.transactionReference}` }),
    ).not.toBeInTheDocument()
    expect(await screen.findByRole('cell', { name: 'Fertiliser price update' })).toBeInTheDocument()
    expect(contributionsApi.listContributionPayments).toHaveBeenCalledWith(70, {
      page: 0,
      size: 20,
      sortBy: 'requestedAt',
      direction: 'desc',
    })
  })

  it('records a payment with a transaction reference made when the dialog opened', async () => {
    logInAs(USERS.finance)
    vi.mocked(paymentsApi.recordPayment).mockResolvedValue({
      payment: pendingPayment,
      created: true,
      message: 'Created',
    })
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    const dialog = await openRecord(user)
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))
    expect(await screen.findByText(/recorded; it counts once confirmed/)).toBeInTheDocument()
    const sent = vi.mocked(paymentsApi.recordPayment).mock.calls[0][0]
    expect(sent).toEqual({
      transactionReference: expect.stringMatching(REFERENCE),
      contributionId: 70,
      amount: 15000,
      method: 'MOBILE_MONEY',
    })
  })

  it('retries after a lost answer with the same reference and details, and never adds a second payment', async () => {
    logInAs(USERS.finance)
    vi.mocked(paymentsApi.recordPayment)
      .mockRejectedValueOnce(new ApiError(0, SERVER_UNREACHABLE))
      .mockResolvedValueOnce({
        payment: pendingPayment,
        created: false,
        message: 'Already received: this is the original payment',
      })
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    const dialog = await openRecord(user)
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))
    expect(await within(dialog).findByText(/may or may not have been recorded/)).toBeInTheDocument()

    // The details lock: changing them would make the retry a different payment (warning on attempt).
    const amount = within(dialog).getByLabelText('Amount')
    await user.type(amount, '9')
    expect(amount).toHaveValue('15000')
    expect(within(dialog).getAllByText(/already sent once with these details/).length).toBeGreaterThan(0)

    await user.click(within(dialog).getByRole('button', { name: 'Retry' }))
    expect(
      await screen.findByText('Already received: the original payment was kept, nothing was added'),
    ).toBeInTheDocument()
    const [first, second] = vi.mocked(paymentsApi.recordPayment).mock.calls.map((call) => call[0])
    expect(second).toEqual(first)
  })

  it('treats a gateway failure (502) like a lost answer: same request on Retry', async () => {
    logInAs(USERS.finance)
    vi.mocked(paymentsApi.recordPayment)
      .mockRejectedValueOnce(new ApiError(502, SERVER_UNREACHABLE))
      .mockResolvedValueOnce({ payment: pendingPayment, created: true, message: 'Created' })
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    const dialog = await openRecord(user)
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))
    await user.click(await within(dialog).findByRole('button', { name: 'Retry' }))
    expect(await screen.findByText(/recorded; it counts once confirmed/)).toBeInTheDocument()
    const [first, second] = vi.mocked(paymentsApi.recordPayment).mock.calls.map((call) => call[0])
    expect(second).toEqual(first)
  })

  it('sends one request when Save is clicked twice', async () => {
    logInAs(USERS.finance)
    let answer
    vi.mocked(paymentsApi.recordPayment).mockImplementation(() => new Promise((resolve) => (answer = resolve)))
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    const dialog = await openRecord(user)
    const save = within(dialog).getByRole('button', { name: 'Save' })
    await user.click(save)
    await user.click(save)
    answer({ payment: pendingPayment, created: true, message: 'Created' })
    expect(await screen.findByText(/recorded; it counts once confirmed/)).toBeInTheDocument()
    expect(paymentsApi.recordPayment).toHaveBeenCalledOnce()
  })

  it('shows the refusal of an amount above what can still be paid (422)', async () => {
    logInAs(USERS.finance)
    vi.mocked(paymentsApi.recordPayment).mockRejectedValue(
      new ApiError(
        422,
        'amount 40000 is more than can still be paid: 30000 outstanding, of which 10000 is already waiting for confirmation.',
      ),
    )
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    const dialog = await openRecord(user)
    await user.click(within(dialog).getByRole('button', { name: 'Save' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('more than can still be paid')
  })

  it('confirms a pending payment with the provider reference, and requires a reason to mark one failed', async () => {
    logInAs(USERS.finance)
    vi.mocked(paymentsApi.confirmPayment).mockResolvedValue({ ...pendingPayment, status: 'CONFIRMED' })
    vi.mocked(paymentsApi.failPayment).mockResolvedValue({ ...pendingPayment, status: 'FAILED' })
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: `Confirm ${pendingPayment.transactionReference}` }))
    let dialog = screen.getByRole('dialog', { name: 'Confirm this payment?' })
    await user.type(within(dialog).getByLabelText('Provider reference'), ' BK-998877 ')
    await user.click(within(dialog).getByRole('button', { name: 'Confirm payment' }))
    expect(await screen.findByText('Payment confirmed; the balance is updated')).toBeInTheDocument()
    expect(paymentsApi.confirmPayment).toHaveBeenCalledWith(302, 'BK-998877')

    await user.click(screen.getByRole('button', { name: `Mark ${pendingPayment.transactionReference} failed` }))
    dialog = screen.getByRole('dialog', { name: 'Mark this payment as failed?' })
    await user.click(within(dialog).getByRole('button', { name: 'Mark failed' }))
    expect(await within(dialog).findByText('Give the reason.')).toBeInTheDocument()
    await user.type(within(dialog).getByLabelText('Reason'), 'Bank reversed the transfer')
    await user.click(within(dialog).getByRole('button', { name: 'Mark failed' }))
    await waitFor(() => expect(paymentsApi.failPayment).toHaveBeenCalledWith(302, 'Bank reversed the transfer'))
  })

  it('adjusts the amount with a reason, and shows the refusal of an unchanged amount (400)', async () => {
    logInAs(USERS.finance)
    vi.mocked(contributionsApi.adjustContribution)
      .mockRejectedValueOnce(new ApiError(400, 'newAmountDue is the same as the current amount (50000).'))
      .mockResolvedValueOnce({ ...contribution, amountDue: 55000 })
    renderRoute('/contributions/70')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Adjust amount' }))
    const dialog = screen.getByRole('dialog', { name: 'Adjust the amount owed' })
    await user.type(within(dialog).getByLabelText('New amount due'), '50000')
    await user.type(within(dialog).getByLabelText('Reason'), 'Price check')
    await user.click(within(dialog).getByRole('button', { name: 'Adjust' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('the same as the current amount')
    const amount = within(dialog).getByLabelText('New amount due')
    await user.clear(amount)
    await user.type(amount, '55000')
    await user.click(within(dialog).getByRole('button', { name: 'Adjust' }))
    expect(await screen.findByText('Amount due changed to 55,000')).toBeInTheDocument()
    expect(contributionsApi.adjustContribution).toHaveBeenLastCalledWith(70, {
      newAmountDue: 55000,
      reason: 'Price check',
    })
  })

  it('shows a farmer their own contribution read-only, without the adjustment history (P2)', async () => {
    logInAs(USERS.farmer)
    renderRoute('/contributions/70')
    expect(await screen.findByRole('region', { name: 'Balance' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← My contributions' })).toHaveAttribute('href', '/my/contributions')
    expect(screen.queryByRole('button', { name: 'Record payment' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Adjustment history' })).not.toBeInTheDocument()
    expect(contributionsApi.listAdjustments).not.toHaveBeenCalled()
  })

  it('shows "Not available to you" for another farmer’s contribution (403)', async () => {
    logInAs(USERS.farmer)
    vi.mocked(contributionsApi.getContribution).mockRejectedValue(
      new ApiError(403, 'You do not have permission to perform this action.'),
    )
    renderRoute('/contributions/71')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
