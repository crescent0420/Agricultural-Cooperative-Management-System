import { useQuery } from '@tanstack/react-query'
import { Navigate, NavLink, useLocation, useParams, useSearchParams } from 'react-router'
import { listSeasons } from '../../api/seasons'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Select } from '../../components/ui/Select'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { NotFoundPage } from '../errors/NotFoundPage'
import { DistrictFilter } from '../reference/DistrictFilter'
import { useDistrictScope } from '../reference/useDistrictScope'
import { useDistrictOptions } from '../reference/useReferenceOptions'
import { CapacityReport } from './CapacityReport'
import { ContributionsReport } from './ContributionsReport'
import { FarmersReport } from './FarmersReport'
import { GradesReport } from './GradesReport'
import { REPORT_TABS } from './reportTabs'

const VIEWS = {
  farmers: FarmersReport,
  capacity: CapacityReport,
  contributions: ContributionsReport,
  grades: GradesReport,
}
const ALL_SEASONS = { page: 0, size: 100, sortBy: 'startsOn', direction: 'desc' }

/**
 * /reports/:report — the management reports as tabs. Loaded on demand (React.lazy in routes.jsx) so the
 * chart library is only downloaded by users who open a report. The filters live in the address
 * (?seasonId=&districtId=) and stay the same when switching tabs.
 */
export default function ReportsPage() {
  const { report } = useParams()
  const location = useLocation()
  const { hasPermission } = useAuth()
  const [search, setSearch] = useSearchParams()
  const tabs = REPORT_TABS.filter((t) => hasPermission(t.permission))

  const seasonFilter = search.get('seasonId') ?? ''
  const districtFilter = search.get('districtId') ?? ''
  const districtId = useDistrictScope(districtFilter)
  const districts = useDistrictOptions()
  const seasons = useQuery({ queryKey: ['seasons', 'options'], queryFn: () => listSeasons(ALL_SEASONS) })

  if (!report) return tabs.length ? <Navigate to={`${tabs[0].path}${location.search}`} replace /> : <ForbiddenPage />
  const tab = REPORT_TABS.find((t) => t.path === report)
  if (!tab) return <NotFoundPage />
  if (!hasPermission(tab.permission)) return <ForbiddenPage />

  const setFilter = (name, value) =>
    setSearch(
      (prev) => {
        const next = new URLSearchParams(prev)
        if (value) next.set(name, value)
        else next.delete(name)
        return next
      },
      { replace: true },
    )
  const names = {
    district: (id) => districts.data?.content.find((d) => d.id === id)?.name,
    season: (id) => seasons.data?.content.find((s) => s.id === id)?.name,
  }
  const filters = { districtId, seasonId: seasonFilter ? Number(seasonFilter) : undefined }
  const View = VIEWS[tab.path]

  return (
    <>
      <PageHeader
        title="Reports"
        description="Management reports, calculated the same way for every district from today's data."
      />
      <nav aria-label="Reports" className="mb-5 flex gap-1 overflow-x-auto border-b border-line print:hidden">
        {tabs.map((t) => (
          <NavLink
            key={t.path}
            to={`/reports/${t.path}${location.search}`}
            className={({ isActive }) =>
              `-mb-px border-b-2 px-4 py-2 text-sm font-semibold whitespace-nowrap ${
                isActive ? 'border-brand-600 text-brand-700' : 'border-transparent text-muted hover:text-ink'
              }`
            }
          >
            {t.label}
          </NavLink>
        ))}
      </nav>
      <div className="mb-5 flex flex-wrap items-start gap-4 rounded-lg border border-line bg-white p-4 print:hidden">
        <DistrictFilter
          value={districtFilter}
          onChange={(v) => setFilter('districtId', v)}
          districts={districts.data?.content}
        />
        {tab.bySeason && (
          <div className="w-full max-w-xs">
            <Select label="Season" value={seasonFilter} onChange={(e) => setFilter('seasonId', e.target.value)}>
              <option value="">All seasons</option>
              {seasons.data?.content.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </Select>
          </div>
        )}
      </div>
      <View filters={filters} names={names} />
    </>
  )
}
