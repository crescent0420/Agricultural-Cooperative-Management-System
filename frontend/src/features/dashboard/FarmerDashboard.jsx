import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listMyContributions } from '../../api/contributions'
import { listMyEnrolments } from '../../api/enrolments'
import { getMyProfile } from '../../api/farmers'
import { listMyPermits } from '../../api/permits'
import { listMyResults } from '../../api/results'
import { listSeasons } from '../../api/seasons'
import { ContributionFlags } from '../contributions/ContributionFlags'
import { EnrolmentStatusBadge } from '../enrolment/EnrolmentStatusBadge'
import { StatusBadge } from '../farmers/StatusBadge'
import { WindowBadge } from '../seasons/WindowBadge'
import { formatDate } from '../../utils/format'
import { CardList, DashboardCard } from './DashboardCard'
import { ALL_SEASONS, currentSeason } from './dashboardQueries'

/** The farmer's own dashboard (URD §17): profile, season, enrolments, balance, results, permits. */
export function FarmerDashboard() {
  const profile = useQuery({ queryKey: ['me', 'profile'], queryFn: getMyProfile })
  const seasons = useQuery({ queryKey: ['seasons', 'options'], queryFn: () => listSeasons(ALL_SEASONS) })
  const enrolments = useQuery({
    queryKey: ['dashboard', 'my-enrolments'],
    queryFn: () => listMyEnrolments({ page: 0, size: 5, sortBy: 'enrolledAt', direction: 'desc' }),
  })
  const balance = useQuery({
    queryKey: ['dashboard', 'my-contributions'],
    queryFn: () => listMyContributions({ page: 0, size: 3, sortBy: 'createdAt', direction: 'desc' }),
  })
  const results = useQuery({
    queryKey: ['dashboard', 'my-results'],
    queryFn: () => listMyResults({ page: 0, size: 3, sortBy: 'id', direction: 'desc' }),
  })
  const permits = useQuery({
    queryKey: ['dashboard', 'my-permits'],
    queryFn: () => listMyPermits({ page: 0, size: 3, sortBy: 'issuedAt', direction: 'desc' }),
  })

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      <DashboardCard title="My profile" query={profile} to="/my/profile" linkLabel="Open my profile">
        {(p) => (
          <div className="space-y-1 text-sm">
            <p className="text-lg font-semibold text-ink">{p.fullName}</p>
            <p className="font-mono text-muted">{p.membershipNumber}</p>
            <p className="text-muted">
              {p.cooperativeName} · {p.districtName}
            </p>
            <StatusBadge status={p.status} />
          </div>
        )}
      </DashboardCard>

      <DashboardCard title="Season" query={seasons}>
        {(page) => {
          const season = currentSeason(page.content)
          if (!season) return <p className="text-sm text-muted">No season has been created yet.</p>
          return (
            <div className="space-y-2 text-sm">
              <p className="text-lg font-semibold text-ink">{season.name}</p>
              <p className="text-muted">
                {formatDate(season.startsOn)} – {formatDate(season.endsOn)} · inputs distributed{' '}
                {formatDate(season.distributionDate)}
              </p>
              <WindowBadge open={season.enrolmentOpen} />
              {season.enrolmentOpen ? (
                <p>
                  <Link to="/my/offerings" className="font-semibold text-brand-700 hover:underline">
                    See the offerings you can enrol in →
                  </Link>
                </p>
              ) : (
                <p className="text-muted">Enrolment is closed for now.</p>
              )}
            </div>
          )
        }}
      </DashboardCard>

      <DashboardCard title="My balance" query={balance} to="/my/contributions" linkLabel="Open my contributions">
        {(page) => (
          <CardList
            empty="No contribution has been set for you yet."
            rows={page.content.map((c) => ({
              key: c.id,
              main: `${c.seasonName}: ${c.paidPercent}% paid`,
              detail: <ContributionFlags contribution={c} />,
            }))}
          />
        )}
      </DashboardCard>

      <DashboardCard title="My enrolments" query={enrolments} to="/my/enrolments" linkLabel="Open my enrolments">
        {(page) => (
          <CardList
            empty="You are not enrolled in any programme."
            rows={page.content.map((e) => ({
              key: e.id,
              main: e.programmeName,
              detail: e.seasonName,
              to: `/enrolments/${e.id}`,
              side: <EnrolmentStatusBadge status={e.status} />,
            }))}
          />
        )}
      </DashboardCard>

      <DashboardCard title="Latest results" query={results} to="/my/results" linkLabel="Open my results">
        {(page) => (
          <CardList
            empty="No result has been published for you yet."
            rows={page.content.map((r) => ({
              key: r.id,
              main: r.programmeName,
              detail: r.seasonName,
              side: (
                <span className="text-sm">
                  <span className="font-semibold text-ink">Grade {r.grade ?? '—'}</span>
                  <span className="block text-xs text-muted">{r.finalScore ?? '—'} points</span>
                </span>
              ),
            }))}
          />
        )}
      </DashboardCard>

      <DashboardCard title="My permits" query={permits} to="/my/permits" linkLabel="Open my permits">
        {(page) => (
          <CardList
            empty="You have no collection permit yet."
            rows={page.content.map((p) => ({
              key: p.id,
              main: <span className="font-mono">{p.permitNumber}</span>,
              detail: `${p.programmeName} · ${p.seasonName}`,
              side: <span className="text-xs text-muted">Collect {formatDate(p.distributionDate)}</span>,
            }))}
          />
        )}
      </DashboardCard>
    </div>
  )
}
