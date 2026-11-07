import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as contributionsApi from '../../api/contributions'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as offeringsApi from '../../api/offerings'
import * as seasonsApi from '../../api/seasons'
import { contribution2026B, uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS, uwase } from '../../test/farmers'
import { paged } from '../../test/paged'
import { renderRoute } from '../../test/renderRoute'
import { fullOffering, maizeOffering, season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/contributions')
vi.mock('../../api/enrolments')
vi.mock('../../api/farmers')
vi.mock('../../api/offerings')
vi.mock('../../api/seasons')

const card = (name) => screen.getByRole('article', { name })

describe('OfferingsForMePage (FR-ENR-01, FR-ENR-02)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    logInAs(USERS.farmer)
    vi.mocked(offeringsApi.listOfferings).mockResolvedValue(paged([maizeOffering, fullOffering]))
    vi.mocked(farmersApi.getMyProfile).mockResolvedValue(uwase)
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
    vi.mocked(contributionsApi.listMyContributions).mockResolvedValue(paged([contribution2026B]))
    vi.mocked(enrolmentsApi.listMyEnrolments).mockResolvedValue(paged([]))
    vi.mocked(farmersApi.getMissingPrerequisites).mockImplementation(async (_, programmeId) =>
      programmeId === 31 ? [{ id: 4, name: 'Pest and disease control' }] : [],
    )
  })

  it('shows each offering with a checklist built from the backend data', async () => {
    renderRoute('/my/offerings')
    await screen.findByRole('article', { name: 'Maize, 2026B' })
    const maize = card('Maize, 2026B')
    await within(maize).findByText('All prerequisite trainings completed.')
    expect(within(maize).getByText('Enrolment for 2026B is open.')).toBeInTheDocument()
    expect(within(maize).getByText('You have paid 40% of the 2026B contribution.')).toBeInTheDocument()
    expect(within(maize).getAllByText('✓')).toHaveLength(5)

    const beans = card('Beans, 2026B')
    expect(await within(beans).findByText('Still needed: Pest and disease control.')).toBeInTheDocument()
    expect(within(beans).getByText('This offering is full.')).toBeInTheDocument()
    expect(within(beans).getByText('Full')).toBeInTheDocument()
    // The checklist is advice only: Enrol stays available (FR-ENR-01).
    expect(within(beans).getByRole('button', { name: 'Enrol' })).toBeEnabled()
    expect(farmersApi.getMissingPrerequisites).toHaveBeenCalledWith(7, 31)
  })

  it('enrols after a confirmation, then marks the offering Enrolled', async () => {
    vi.mocked(enrolmentsApi.enrol).mockResolvedValue(uwaseMaize)
    renderRoute('/my/offerings')
    const user = userEvent.setup()
    await screen.findByRole('article', { name: 'Maize, 2026B' })
    await user.click(within(card('Maize, 2026B')).getByRole('button', { name: 'Enrol' }))
    const dialog = screen.getByRole('dialog', { name: 'Enrol in Maize, 2026B?' })
    expect(dialog).toHaveTextContent('You will farm 0.5 ha')

    vi.mocked(enrolmentsApi.listMyEnrolments).mockResolvedValue(paged([uwaseMaize])) // what a reload returns
    await user.click(within(dialog).getByRole('button', { name: 'Enrol' }))
    expect(await screen.findByText('Enrolled in Maize, 2026B (0.5 ha)')).toBeInTheDocument()
    expect(enrolmentsApi.enrol).toHaveBeenCalledWith(12)
    await waitFor(() => expect(within(card('Maize, 2026B')).getByText('Enrolled')).toBeInTheDocument())
    expect(within(card('Maize, 2026B')).queryByRole('button', { name: 'Enrol' })).not.toBeInTheDocument()
  })

  it.each([
    [409, 'This programme offering is full.'],
    [422, "This enrolment would bring the farmer's land in season '2026B' to 3.0 ha; the limit is 2.5 ha."],
  ])('shows the backend refusal (%i) in the dialog', async (status, message) => {
    vi.mocked(enrolmentsApi.enrol).mockRejectedValue(new ApiError(status, message))
    renderRoute('/my/offerings')
    const user = userEvent.setup()
    await screen.findByRole('article', { name: 'Beans, 2026B' })
    await user.click(within(card('Beans, 2026B')).getByRole('button', { name: 'Enrol' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: 'Enrol' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(message)
  })

  it('says when no contribution is recorded for the season', async () => {
    vi.mocked(contributionsApi.listMyContributions).mockResolvedValue(paged([]))
    renderRoute('/my/offerings')
    await screen.findByRole('article', { name: 'Maize, 2026B' })
    expect(
      await within(card('Maize, 2026B')).findByText('No input contribution is recorded for you in 2026B.'),
    ).toBeInTheDocument()
  })

  it('shows the offerings whose requirements are all met first, then by how many are met', async () => {
    const beans = { ...maizeOffering, id: 14, programmeId: 31, programmeName: 'Beans', remainingPlaces: 5 }
    const sorghum = { ...maizeOffering, id: 15, programmeId: 32, programmeName: 'Sorghum', remainingPlaces: 0 }
    // The backend's order (newest first): Sorghum (2 missing: prerequisite and place), Beans (1), Maize (none).
    vi.mocked(offeringsApi.listOfferings).mockResolvedValue(paged([sorghum, beans, maizeOffering]))
    vi.mocked(farmersApi.getMissingPrerequisites).mockImplementation(async (_, programmeId) =>
      programmeId === 30 ? [] : [{ id: 4, name: 'Pest and disease control' }],
    )
    renderRoute('/my/offerings')
    await screen.findByRole('article', { name: 'Maize, 2026B' })
    const order = screen.getAllByRole('article').map((a) => a.getAttribute('aria-label'))
    expect(order).toEqual(['Maize, 2026B', 'Beans, 2026B', 'Sorghum, 2026B'])
    expect(within(card('Maize, 2026B')).getByText('5 of 5 met')).toBeInTheDocument()
    expect(within(card('Sorghum, 2026B')).getByText('3 of 5 met')).toBeInTheDocument()
  })

  it('loads every page of offerings so that all of them are ordered', async () => {
    vi.mocked(offeringsApi.listOfferings)
      .mockResolvedValueOnce({ ...paged([fullOffering]), totalPages: 2 })
      .mockResolvedValueOnce({ ...paged([maizeOffering]), page: 1, totalPages: 2 })
    renderRoute('/my/offerings')
    await screen.findByRole('article', { name: 'Maize, 2026B' })
    expect(screen.getAllByRole('article').map((a) => a.getAttribute('aria-label'))).toEqual([
      'Maize, 2026B',
      'Beans, 2026B',
    ])
    expect(offeringsApi.listOfferings).toHaveBeenCalledWith(
      expect.objectContaining({ page: 1, size: 100 }),
      expect.anything(),
    )
  })

  it('is for farmers only', async () => {
    logInAs(USERS.registrar)
    renderRoute('/my/offerings')
    expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
  })
})
