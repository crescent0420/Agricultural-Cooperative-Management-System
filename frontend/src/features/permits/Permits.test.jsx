import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as permitsApi from '../../api/permits'
import * as seasonsApi from '../../api/seasons'
import { uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { issuedAttempt, permit, refusedAttempt } from '../../test/permits'
import { renderRoute } from '../../test/renderRoute'
import { season2026B } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/enrolments')
vi.mock('../../api/farmers')
vi.mock('../../api/permits')
vi.mock('../../api/seasons')

describe('Get permit (FR-PER-01)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.farmer)
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2026B]))
    vi.mocked(enrolmentsApi.listMyEnrolments).mockResolvedValue(paged([uwaseMaize]))
  })

  async function getPermit() {
    renderRoute('/my/enrolments')
    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Get the permit for Maize' }))
    return screen.getByRole('dialog', { name: 'Collection permit: Maize, 2026B' })
  }

  it('shows a newly issued permit', async () => {
    vi.mocked(permitsApi.generatePermit).mockResolvedValue({ permit, created: true })
    const dialog = await getPermit()
    expect(await within(dialog).findByText('CP-AB2C-D3EF-GH4J')).toBeInTheDocument()
    expect(within(dialog).getByText(/has been issued/)).toBeInTheDocument()
    expect(permitsApi.generatePermit).toHaveBeenCalledOnce()
    expect(permitsApi.generatePermit).toHaveBeenCalledWith(50)
  })

  it('shows the same permit again when one was already issued', async () => {
    vi.mocked(permitsApi.generatePermit).mockResolvedValue({ permit, created: false })
    const dialog = await getPermit()
    expect(await within(dialog).findByText(/already have this permit/)).toBeInTheDocument()
    expect(within(dialog).getByText('CP-AB2C-D3EF-GH4J')).toBeInTheDocument()
  })

  it('shows the backend’s reason when the permit is refused (422)', async () => {
    vi.mocked(permitsApi.generatePermit).mockRejectedValue(
      new ApiError(422, "Inputs can be collected only when the season's contribution is fully paid; 40% is paid."),
    )
    const dialog = await getPermit()
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('fully paid')
  })

  it('is offered on the enrolment page to the farmer only', async () => {
    vi.mocked(enrolmentsApi.getEnrolment).mockResolvedValue(uwaseMaize)
    renderRoute('/enrolments/50')
    expect(await screen.findByRole('button', { name: 'Get permit' })).toBeInTheDocument()
  })
})

describe('MyPermitsPage (FR-PER-02)', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows each permit with its number and distribution day, and prints', async () => {
    logInAs(USERS.farmer)
    vi.mocked(permitsApi.listMyPermits).mockResolvedValue(paged([permit]))
    const print = vi.spyOn(window, 'print').mockImplementation(() => {})
    renderRoute('/my/permits')
    const card = await screen.findByRole('article', { name: 'Permit CP-AB2C-D3EF-GH4J' })
    expect(within(card).getByText('15 Dec 2026')).toBeInTheDocument()
    expect(within(card).getByText('Maize, 2026B')).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Print' }))
    expect(print).toHaveBeenCalledOnce()
    print.mockRestore()
  })
})

describe('VerifyPermitPage (FR-PER-03)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.finance)
  })

  async function check(text) {
    const user = userEvent.setup()
    const field = await screen.findByLabelText('Permit number')
    await user.clear(field)
    await user.type(field, text)
    await user.click(screen.getByRole('button', { name: 'Check' }))
  }

  it('catches a badly formatted number before sending it', async () => {
    renderRoute('/permits/verify')
    await check('CP-AB0C-D3EF-GH4J')
    expect(await screen.findByText(/never contain I, L, O, 0 or 1/)).toBeInTheDocument()
    expect(permitsApi.verifyPermit).not.toHaveBeenCalled()
  })

  it('shows a large VALID for a good permit, after tidying what was typed', async () => {
    vi.mocked(permitsApi.verifyPermit).mockResolvedValue({ valid: true, problems: [], permit })
    renderRoute('/permits/verify')
    await check('cp ab2c d3ef gh4j')
    const result = await screen.findByRole('region', { name: 'Verification result' })
    expect(result).toHaveTextContent('✓ VALID')
    expect(permitsApi.verifyPermit).toHaveBeenCalledWith('CP-AB2C-D3EF-GH4J')
    expect(screen.getByRole('article', { name: 'Permit CP-AB2C-D3EF-GH4J' })).toBeInTheDocument()
  })

  it('shows NOT VALID with the backend’s problems', async () => {
    vi.mocked(permitsApi.verifyPermit).mockResolvedValue({
      valid: false,
      problems: ['The farmer is SUSPENDED, not Active.', 'The distribution day (2026-12-15) has passed.'],
      permit,
    })
    renderRoute('/permits/verify')
    await check('CP-AB2C-D3EF-GH4J')
    const result = await screen.findByRole('region', { name: 'Verification result' })
    expect(result).toHaveTextContent('✕ NOT VALID')
    expect(within(result).getAllByRole('listitem')).toHaveLength(2)
  })

  it('says when no permit has the number (404)', async () => {
    vi.mocked(permitsApi.verifyPermit).mockRejectedValue(
      new ApiError(404, 'No collection permit has the number CP-AB2C-D3EF-GH4J.'),
    )
    renderRoute('/permits/verify')
    await check('CP-AB2C-D3EF-GH4J')
    expect(await screen.findByText('No permit has the number CP-AB2C-D3EF-GH4J.')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Verification result' })).toHaveTextContent('NOT VALID')
  })
})

describe('PermitAttemptsPage (FR-PER-04)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(permitsApi.listPermitAttempts).mockResolvedValue(paged([issuedAttempt, refusedAttempt]))
  })

  it('lists every attempt with its outcome and the reason for refusals; filters by outcome and enrolment', async () => {
    logInAs(USERS.registrar)
    renderRoute('/permits/attempts')
    expect(await screen.findByRole('cell', { name: 'The contribution is not fully paid (40%).' })).toBeInTheDocument()
    const table = screen.getByRole('table')
    expect(within(table).getByText('Refused')).toBeInTheDocument()
    expect(within(table).getByText('Issued')).toBeInTheDocument()
    expect(within(table).getAllByRole('link', { name: 'Farmer #7' })[0]).toHaveAttribute('href', '/farmers/7')

    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText('Outcome'), 'Refused')
    await user.type(screen.getByLabelText('Enrolment number'), '5a0')
    await user.click(screen.getByRole('button', { name: 'Filter' }))
    await waitFor(() =>
      expect(permitsApi.listPermitAttempts).toHaveBeenLastCalledWith(expect.anything(), {
        farmerId: undefined,
        enrolmentId: 50,
        outcome: 'REFUSED',
      }),
    )
  })

  it('shows the Administrator the farmer without a link (T1, G6)', async () => {
    logInAs(USERS.admin)
    vi.mocked(farmersApi.searchFarmers).mockResolvedValue(paged([]))
    renderRoute('/permits/attempts')
    expect((await screen.findAllByText('Farmer #7')).length).toBe(2)
    expect(screen.queryByRole('link', { name: 'Farmer #7' })).not.toBeInTheDocument()
  })

  it('is not available to the Finance Officer', async () => {
    logInAs(USERS.finance)
    renderRoute('/permits/attempts')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
