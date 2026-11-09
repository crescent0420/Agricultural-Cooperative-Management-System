import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as auditApi from '../../api/audit'
import * as contributionsApi from '../../api/contributions'
import * as enrolmentsApi from '../../api/enrolments'
import { ApiError } from '../../api/errors'
import * as farmersApi from '../../api/farmers'
import * as offeringsApi from '../../api/offerings'
import * as permitsApi from '../../api/permits'
import * as reportsApi from '../../api/reports'
import * as resultsApi from '../../api/results'
import * as seasonsApi from '../../api/seasons'
import * as usersApi from '../../api/users'
import { adminAccount, auditEvents, officerAccount, unlinkedFarmerAccount } from '../../test/admin'
import { contribution2026B, uwaseMaize } from '../../test/enrolments'
import { logInAs, USERS, uwase } from '../../test/farmers'
import { paged } from '../../test/paged'
import { permit } from '../../test/permits'
import { capacityReport, contributionsReport, farmersReport, gradesReport } from '../../test/reports'
import { publishedResult, summaryOf } from '../../test/results'
import { renderRoute } from '../../test/renderRoute'
import { fullOffering, maizeOffering, season2026B, season2027A } from '../../test/seasons'
import { currentSeason } from './dashboardQueries'

vi.mock('../../api/auth')
vi.mock('../../api/audit')
vi.mock('../../api/contributions')
vi.mock('../../api/enrolments')
vi.mock('../../api/farmers')
vi.mock('../../api/offerings')
vi.mock('../../api/permits')
vi.mock('../../api/reports')
vi.mock('../../api/results')
vi.mock('../../api/seasons')
vi.mock('../../api/users')

const card = (name) => screen.findByRole('region', { name })

/** Answers the per-offering count requests: offering id → number of results with that status. */
function countsByOffering(counts) {
  vi.mocked(resultsApi.listOfferingResults).mockImplementation(async (offeringId) =>
    paged([], 0, 1, counts[offeringId] ?? 0),
  )
}

describe('Dashboards (URD §17)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  describe('Farmer', () => {
    beforeEach(() => {
      logInAs(USERS.farmer)
      vi.mocked(farmersApi.getMyProfile).mockResolvedValue(uwase)
      vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
      vi.mocked(enrolmentsApi.listMyEnrolments).mockResolvedValue(paged([uwaseMaize]))
      vi.mocked(contributionsApi.listMyContributions).mockResolvedValue(paged([contribution2026B]))
      vi.mocked(resultsApi.listMyResults).mockResolvedValue(paged([summaryOf(publishedResult)]))
      vi.mocked(permitsApi.listMyPermits).mockResolvedValue(paged([permit]))
    })

    it('shows the profile, the open season, balance, enrolments, results and permits, each linking on', async () => {
      renderRoute('/')
      const profile = await card('My profile')
      expect(await within(profile).findByText('RCU-000007')).toBeInTheDocument()
      expect(within(profile).getByText('Active')).toBeInTheDocument()
      expect(within(profile).getByRole('link', { name: /Open my profile/ })).toHaveAttribute('href', '/my/profile')

      const balance = screen.getByRole('region', { name: 'My balance' })
      expect(await within(balance).findByText('2026B: 40% paid')).toBeInTheDocument()
      expect(within(balance).getByText('May enrol')).toBeInTheDocument()
      expect(within(balance).getByText('Cannot collect yet')).toBeInTheDocument()

      const enrolments = screen.getByRole('region', { name: 'My enrolments' })
      expect(await within(enrolments).findByRole('link', { name: 'Maize' })).toHaveAttribute(
        'href',
        `/enrolments/${uwaseMaize.id}`,
      )
      const results = screen.getByRole('region', { name: 'Latest results' })
      expect(await within(results).findByText(`Grade ${publishedResult.grade}`)).toBeInTheDocument()
      const permits = screen.getByRole('region', { name: 'My permits' })
      expect(await within(permits).findByText(permit.permitNumber)).toBeInTheDocument()

      expect(enrolmentsApi.listMyEnrolments).toHaveBeenCalledWith({
        page: 0,
        size: 5,
        sortBy: 'enrolledAt',
        direction: 'desc',
      })
    })

    it('shows the season with an open window and offers the offerings (decision D2)', async () => {
      renderRoute('/')
      const season = await card('Season')
      expect(await within(season).findByText('2026B')).toBeInTheDocument()
      expect(within(season).getByText('Enrolment open')).toBeInTheDocument()
      expect(within(season).getByRole('link', { name: /See the offerings/ })).toHaveAttribute('href', '/my/offerings')
    })

    it('keeps the other cards when one request fails', async () => {
      vi.mocked(permitsApi.listMyPermits).mockRejectedValue(new ApiError(500, 'The ACMS server could not be reached.'))
      renderRoute('/')
      const permits = await card('My permits')
      expect(await within(permits).findByRole('alert')).toHaveTextContent('could not be reached')
      expect(
        await within(screen.getByRole('region', { name: 'My profile' })).findByText('RCU-000007'),
      ).toBeInTheDocument()
    })

    it('says so when there is nothing yet', async () => {
      vi.mocked(enrolmentsApi.listMyEnrolments).mockResolvedValue(paged([]))
      vi.mocked(resultsApi.listMyResults).mockResolvedValue(paged([]))
      renderRoute('/')
      expect(await screen.findByText('You are not enrolled in any programme.')).toBeInTheDocument()
      expect(await screen.findByText('No result has been published for you yet.')).toBeInTheDocument()
    })
  })

  it('picks the open season, otherwise the newest (decision D2)', () => {
    expect(currentSeason([season2027A, season2026B])).toBe(season2026B)
    const closed = { ...season2026B, enrolmentOpen: false }
    expect(currentSeason([season2027A, closed])).toBe(season2027A)
    expect(currentSeason([])).toBeNull()
  })

  describe('Field Officer', () => {
    beforeEach(() => {
      logInAs(USERS.officer)
      vi.mocked(offeringsApi.listMyOfferings).mockResolvedValue(paged([maizeOffering, fullOffering]))
    })

    it('lists the own offerings with enrolled / maximum and links permit verification', async () => {
      countsByOffering({})
      renderRoute('/')
      const offerings = await card('My offerings')
      expect(await within(offerings).findByText('40 / 40')).toBeInTheDocument()
      expect(within(offerings).getByText('12 / 40')).toBeInTheDocument()
      expect(within(screen.getByRole('region', { name: 'Collection permits' })).getByRole('link')).toHaveAttribute(
        'href',
        '/permits/verify',
      )
    })

    it('counts the draft results per offering, one small request each (decision D1)', async () => {
      countsByOffering({ 12: 3 })
      renderRoute('/')
      const drafts = await card('Results still in draft')
      // the total and the offering's own count
      expect(await within(drafts).findAllByText('3')).toHaveLength(2)
      expect(within(drafts).getByRole('link', { name: 'Maize · 2026B' })).toHaveAttribute(
        'href',
        '/offerings/12/results?status=DRAFT',
      )
      expect(within(drafts).queryByRole('link', { name: 'Beans · 2026B' })).not.toBeInTheDocument()
      expect(resultsApi.listOfferingResults).toHaveBeenCalledWith(
        12,
        { page: 0, size: 1, sortBy: 'id', direction: 'asc' },
        'DRAFT',
      )
      expect(resultsApi.listOfferingResults).toHaveBeenCalledTimes(2)
    })

    it('says when more offerings exist than are checked', async () => {
      countsByOffering({})
      vi.mocked(offeringsApi.listMyOfferings).mockResolvedValue(paged([maizeOffering], 0, 100, 140))
      renderRoute('/')
      expect(await screen.findByText('Only the 100 newest of 140 offerings are checked.')).toBeInTheDocument()
      expect(screen.getByText('No draft results: every result is submitted.')).toBeInTheDocument()
    })
  })

  describe('District Agronomist', () => {
    beforeEach(() => {
      logInAs(USERS.agronomist)
      vi.mocked(offeringsApi.listOfferings).mockResolvedValue(paged([maizeOffering, fullOffering]))
      vi.mocked(reportsApi.getGradeDistribution).mockResolvedValue(gradesReport)
    })

    it('counts the results waiting for approval across the district', async () => {
      countsByOffering({ 12: 2, 13: 5 })
      renderRoute('/')
      const waiting = await card('Waiting for your approval')
      expect(await within(waiting).findByText('7')).toBeInTheDocument()
      expect(within(waiting).getByRole('link', { name: 'Beans · 2026B' })).toHaveAttribute(
        'href',
        '/offerings/13/results?status=SUBMITTED',
      )
      expect(offeringsApi.listOfferings).toHaveBeenCalledWith(
        { page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' },
        { districtId: 15 },
      )
    })

    it('shows the district grade figures from the report', async () => {
      countsByOffering({})
      renderRoute('/')
      const grades = await card('Grades in your district')
      expect(await within(grades).findByText('A 33.33% · B 66.67% · C 0%')).toBeInTheDocument()
      expect(reportsApi.getGradeDistribution).toHaveBeenCalledWith({ districtId: 15 })
      expect(screen.getByText('Nothing is waiting for approval.')).toBeInTheDocument()
    })
  })

  describe('Union Registrar', () => {
    beforeEach(() => {
      logInAs(USERS.registrar)
      vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
      vi.mocked(reportsApi.getFarmersPerCooperative).mockResolvedValue(farmersReport)
      vi.mocked(reportsApi.getOfferingCapacity).mockResolvedValue(capacityReport)
      vi.mocked(reportsApi.getOutstandingContributions).mockResolvedValue(contributionsReport)
    })

    it('lists only the seasons with an open window and the Union figures', async () => {
      renderRoute('/')
      const windows = await card('Enrolment windows')
      expect(await within(windows).findByRole('link', { name: '2026B' })).toHaveAttribute(
        'href',
        `/seasons/${season2026B.id}`,
      )
      expect(within(windows).queryByText('2027A')).not.toBeInTheDocument()
      expect(await within(screen.getByRole('region', { name: 'Farmers' })).findByText('127')).toBeInTheDocument()
      expect(
        await within(screen.getByRole('region', { name: 'Offering capacity' })).findByText('74.29%'),
      ).toBeInTheDocument()
      expect(
        await within(screen.getByRole('region', { name: 'Contributions' })).findByText('649,999.5'),
      ).toBeInTheDocument()
    })

    it('shows one failing report in its own card only', async () => {
      vi.mocked(reportsApi.getOfferingCapacity).mockRejectedValue(new ApiError(500, 'Report unavailable.'))
      renderRoute('/')
      const capacity = await card('Offering capacity')
      expect(await within(capacity).findByRole('alert')).toHaveTextContent('Report unavailable.')
      expect(await within(screen.getByRole('region', { name: 'Farmers' })).findByText('127')).toBeInTheDocument()
    })
  })

  describe('Finance Officer', () => {
    it('shows what is outstanding per season and district, and the latest contributions', async () => {
      logInAs(USERS.finance)
      vi.mocked(reportsApi.getOutstandingContributions).mockResolvedValue(contributionsReport)
      vi.mocked(contributionsApi.listContributions).mockResolvedValue(paged([contribution2026B]))
      renderRoute('/')
      const perSeason = await card('Outstanding per season')
      expect(await within(perSeason).findByText('2026B · Huye')).toBeInTheDocument()
      expect(within(perSeason).getByText('649,999.5 RWF')).toBeInTheDocument()
      const latest = screen.getByRole('region', { name: 'Latest contributions' })
      expect(await within(latest).findByRole('link', { name: 'Uwase Marie' })).toHaveAttribute(
        'href',
        '/contributions/70',
      )
      expect(within(latest).getByText('30,000 RWF')).toBeInTheDocument()
      expect(contributionsApi.listContributions).toHaveBeenCalledWith({
        page: 0,
        size: 5,
        sortBy: 'createdAt',
        direction: 'desc',
      })
      expect(reportsApi.getOfferingCapacity).not.toHaveBeenCalled()
    })
  })

  describe('System Administrator', () => {
    it('shows the number of accounts with the newest, and the latest audit events (decision D3)', async () => {
      logInAs(USERS.admin)
      vi.mocked(usersApi.listUsers).mockResolvedValue(
        paged([unlinkedFarmerAccount, { ...officerAccount, enabled: false }, adminAccount], 0, 5, 42),
      )
      vi.mocked(auditApi.listAuditLogs).mockResolvedValue(paged(auditEvents))
      renderRoute('/')
      const accounts = await card('Accounts')
      expect(await within(accounts).findByText('42')).toBeInTheDocument()
      expect(within(accounts).getByText('Not linked')).toBeInTheDocument()
      expect(within(accounts).getByText('Disabled')).toBeInTheDocument()
      const events = screen.getByRole('region', { name: 'Latest audit events' })
      expect(await within(events).findByText('Login failure')).toBeInTheDocument()
      expect(within(events).getByText('Failed')).toBeInTheDocument()
      await waitFor(() =>
        expect(auditApi.listAuditLogs).toHaveBeenCalledWith({
          page: 0,
          size: 8,
          sortBy: 'occurredAt',
          direction: 'desc',
        }),
      )
    })
  })
})
