import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import * as referenceApi from '../../api/reference'
import * as reportsApi from '../../api/reports'
import * as seasonsApi from '../../api/seasons'
import { huye, logInAs, musanze, USERS } from '../../test/farmers'
import { paged } from '../../test/paged'
import { capacityReport, contributionsReport, farmersReport, gradesReport } from '../../test/reports'
import { renderRoute } from '../../test/renderRoute'
import { season2026B, season2027A } from '../../test/seasons'

vi.mock('../../api/auth')
vi.mock('../../api/reference')
vi.mock('../../api/reports')
vi.mock('../../api/seasons')

const tabNames = () =>
  within(screen.getByRole('navigation', { name: 'Reports' }))
    .getAllByRole('link')
    .map((l) => l.textContent)

/** A headline number: the value under its label. */
const tile = (label) => screen.getByText(label, { selector: 'dt' }).nextElementSibling.textContent

/** The cells of the table's footer row ("Total" first). */
const totalsRow = (table) =>
  within(table.querySelector('tfoot'))
    .getAllByRole('cell')
    .map((c) => c.textContent)

describe('Reports page (FR-REP-01..04)', () => {
  // The page is loaded on demand (React.lazy) together with the chart library. Load it once here, with a long
  // time limit, so a slow first compile is not counted against the 5 s each test waits for the page.
  beforeAll(async () => {
    await import('./ReportsPage')
  }, 60_000)

  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(referenceApi.listDistricts).mockResolvedValue(paged([huye, musanze]))
    vi.mocked(seasonsApi.listSeasons).mockResolvedValue(paged([season2027A, season2026B]))
    vi.mocked(reportsApi.getFarmersPerCooperative).mockResolvedValue(farmersReport)
    vi.mocked(reportsApi.getOfferingCapacity).mockResolvedValue(capacityReport)
    vi.mocked(reportsApi.getOutstandingContributions).mockResolvedValue(contributionsReport)
    vi.mocked(reportsApi.getGradeDistribution).mockResolvedValue(gradesReport)
  })

  describe('tabs follow the permissions', () => {
    it('opens the first report the Registrar may see and shows all four tabs', async () => {
      logInAs(USERS.registrar)
      const { router } = renderRoute('/reports')
      expect(await screen.findByRole('heading', { name: 'Farmers per cooperative' })).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/reports/farmers')
      expect(tabNames()).toEqual([
        'Farmers per cooperative',
        'Offering capacity',
        'Outstanding contributions',
        'Grade distribution',
      ])
    })

    it('opens Outstanding contributions for the Finance Officer, the only report they hold', async () => {
      logInAs(USERS.finance)
      const { router } = renderRoute('/reports')
      expect(await screen.findByRole('heading', { name: 'Outstanding contributions per season' })).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/reports/contributions')
      expect(tabNames()).toEqual(['Outstanding contributions'])
      expect(reportsApi.getFarmersPerCooperative).not.toHaveBeenCalled()
    })

    it('shows a Field Officer three tabs and refuses the contributions report typed in the address', async () => {
      logInAs(USERS.officer)
      renderRoute('/reports/contributions')
      expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
      expect(reportsApi.getOutstandingContributions).not.toHaveBeenCalled()
    })

    it('is not available to a farmer', async () => {
      logInAs(USERS.farmer)
      renderRoute('/reports')
      expect(await screen.findByRole('heading', { name: 'Not available to you' })).toBeInTheDocument()
    })

    it('answers an unknown report with "Page not found"', async () => {
      logInAs(USERS.registrar)
      renderRoute('/reports/profits')
      expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    })
  })

  describe('farmers per cooperative', () => {
    it('shows the headline numbers, the chart and every cooperative with the backend totals', async () => {
      logInAs(USERS.registrar)
      renderRoute('/reports/farmers')
      expect(await screen.findByText('Farmers per cooperative, by status')).toBeInTheDocument()
      expect(tile('Cooperatives')).toBe('2')
      expect(tile('Farmers')).toBe('127')
      expect(tile('Suspended')).toBe('4')
      const legend = screen.getByRole('list', { name: 'Legend' })
      expect(
        within(legend)
          .getAllByRole('listitem')
          .map((li) => li.textContent),
      ).toEqual(['Active', 'Suspended', 'Withdrawn', 'Retired'])
      const table = screen.getByRole('table', { name: 'Farmers per cooperative' })
      // A cooperative without farmers is still listed (backend basis).
      expect(within(table).getByRole('cell', { name: 'Musanze Potato Cooperative' })).toBeInTheDocument()
      expect(totalsRow(table)).toEqual(['Total', '2 cooperatives', '120', '4', '2', '1', '127'])
      expect(screen.getByText('Generated', { exact: false })).toBeInTheDocument()
      expect(screen.getByText('District: all districts')).toBeInTheDocument()
    })

    it('shows how the report is calculated (the backend basis)', async () => {
      logInAs(USERS.registrar)
      renderRoute('/reports/farmers')
      await userEvent.setup().click(await screen.findByText('How this report is calculated'))
      expect(screen.getByText('Farmers per cooperative: how it is calculated.')).toBeVisible()
    })

    it('sends the chosen district and keeps it in the address; this report has no season filter', async () => {
      logInAs(USERS.registrar)
      vi.mocked(reportsApi.getFarmersPerCooperative).mockImplementation(async ({ districtId }) => ({
        ...farmersReport,
        scope: { ...farmersReport.scope, districtId: districtId ?? null },
      }))
      const { router } = renderRoute('/reports/farmers')
      const user = userEvent.setup()
      await screen.findByRole('option', { name: 'Huye' })
      expect(screen.queryByLabelText('Season')).not.toBeInTheDocument()
      await user.selectOptions(screen.getByLabelText('District'), 'Huye')

      await waitFor(() => expect(reportsApi.getFarmersPerCooperative).toHaveBeenLastCalledWith({ districtId: 15 }))
      expect(router.state.location.search).toBe('?districtId=15')
      expect(await screen.findByText('District: Huye')).toBeInTheDocument()
    })

    it('locks district staff to their own district', async () => {
      logInAs(USERS.agronomist)
      renderRoute('/reports/farmers')
      await screen.findByRole('heading', { name: 'Farmers per cooperative' })
      expect(screen.getByLabelText('District')).toBeDisabled()
      expect(reportsApi.getFarmersPerCooperative).toHaveBeenCalledWith({ districtId: 15 })
    })

    it('says so when there is nothing to report', async () => {
      logInAs(USERS.registrar)
      vi.mocked(reportsApi.getFarmersPerCooperative).mockResolvedValue({
        ...farmersReport,
        rows: [],
        totals: { cooperatives: 0, active: 0, suspended: 0, withdrawn: 0, retired: 0, total: 0 },
      })
      renderRoute('/reports/farmers')
      expect(await screen.findByText('There are no cooperatives in this district.')).toBeInTheDocument()
      expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })

    it('shows the backend message in red when the report is refused', async () => {
      logInAs(USERS.registrar)
      vi.mocked(reportsApi.getFarmersPerCooperative).mockRejectedValue(
        new ApiError(403, 'You do not have permission to perform this action.'),
      )
      renderRoute('/reports/farmers')
      expect(await screen.findByRole('alert')).toHaveTextContent('You do not have permission to perform this action.')
    })
  })

  describe('offering capacity', () => {
    it('shows capacity used, marks full offerings and links each offering for the Registrar', async () => {
      logInAs(USERS.registrar)
      renderRoute('/reports/capacity')
      const table = await screen.findByRole('table', { name: 'Enrolments and capacity per offering' })
      expect(within(table).getByRole('link', { name: 'Maize' })).toHaveAttribute('href', '/offerings/9')
      expect(within(table).getByText('Full')).toBeInTheDocument()
      expect(totalsRow(table)).toEqual(['Total', '2 offerings', '', '', '70', '52', '3', '18', '74.29%', '64.5'])
      expect(tile('Capacity used')).toBe('74.29%')
      expect(screen.getByText('of 70 places')).toBeInTheDocument()
      expect(screen.getByText('Enrolled and places left per offering')).toBeInTheDocument()
    })

    it('sends the chosen season and keeps the filters when switching tabs', async () => {
      logInAs(USERS.registrar)
      renderRoute('/reports/capacity')
      const user = userEvent.setup()
      await screen.findByRole('option', { name: '2026B' })
      await user.selectOptions(screen.getByLabelText('Season'), '2026B')

      await waitFor(() =>
        expect(reportsApi.getOfferingCapacity).toHaveBeenLastCalledWith({ seasonId: 3, districtId: undefined }),
      )
      expect(await screen.findByText('District: all districts · Season: 2026B')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Grade distribution' })).toHaveAttribute(
        'href',
        '/reports/grades?seasonId=3',
      )
    })

    it('tells a Field Officer that only their own offerings are counted', async () => {
      logInAs(USERS.officer)
      vi.mocked(reportsApi.getOfferingCapacity).mockResolvedValue({
        ...capacityReport,
        scope: { districtId: 15, seasonId: null, fieldOfficerId: 5 },
      })
      renderRoute('/reports/capacity')
      expect(await screen.findByText('Only the offerings assigned to you are counted.')).toBeInTheDocument()
      expect(tabNames()).toEqual(['Farmers per cooperative', 'Offering capacity', 'Grade distribution'])
      expect(reportsApi.getOfferingCapacity).toHaveBeenCalledWith({ seasonId: undefined, districtId: 15 })
    })
  })

  describe('outstanding contributions', () => {
    it('shows the money in RWF from the totals, with pending payments apart', async () => {
      logInAs(USERS.finance)
      renderRoute('/reports/contributions')
      expect(await screen.findByText('Amount due (RWF)')).toBeInTheDocument()
      expect(tile('Amount due (RWF)')).toBe('2,600,000')
      expect(tile('Outstanding (RWF)')).toBe('649,999.5')
      expect(screen.getByText('not counted as paid yet')).toBeInTheDocument()
      expect(screen.getByText('15 partly · 7 unpaid')).toBeInTheDocument()
      const table = screen.getByRole('table', { name: 'Outstanding contributions per season and district' })
      expect(totalsRow(table)).toEqual([
        'Total',
        '',
        '52',
        '2,600,000',
        '1,950,000.5',
        '100,000',
        '649,999.5',
        '75%',
        '30',
        '15',
        '7',
      ])
    })
  })

  describe('grade distribution', () => {
    it('makes one column per grade of the totals, with count and share', async () => {
      logInAs(USERS.agronomist)
      renderRoute('/reports/grades')
      const table = await screen.findByRole('table', { name: 'Grade distribution per offering' })
      const headers = within(table)
        .getAllByRole('columnheader')
        .map((h) => h.textContent)
      expect(headers.slice(-3)).toEqual(['Grade A', 'Grade B', 'Grade C'])
      expect(totalsRow(table).slice(-3)).toEqual(['1 (33.33%)', '2 (66.67%)', '0 (0%)'])
      expect(screen.getByText('Published results per grade')).toBeInTheDocument()
      expect(screen.getByText('Shares: A 33.33% · B 66.67% · C 0%')).toBeInTheDocument()
    })

    it('explains that there is no distribution before any result is published', async () => {
      logInAs(USERS.registrar)
      const none = [
        { grade: 'A', count: 0, percent: null },
        { grade: 'B', count: 0, percent: null },
      ]
      vi.mocked(reportsApi.getGradeDistribution).mockResolvedValue({
        ...gradesReport,
        rows: [{ ...gradesReport.rows[0], published: 0, notYetPublished: 40, averageFinalScore: null, grades: none }],
        totals: { offerings: 1, published: 0, notYetPublished: 40, averageFinalScore: null, grades: none },
      })
      renderRoute('/reports/grades')
      expect(
        await screen.findByText('No results are published yet, so there is no grade distribution to show.'),
      ).toBeInTheDocument()
      const table = screen.getByRole('table', { name: 'Grade distribution per offering' })
      expect(totalsRow(table).slice(-2)).toEqual(['0 (—)', '0 (—)'])
    })
  })
})
