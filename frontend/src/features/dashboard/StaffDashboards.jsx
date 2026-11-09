import { useQuery } from '@tanstack/react-query'
import { listAuditLogs } from '../../api/audit'
import { listContributions } from '../../api/contributions'
import { listMyOfferings, listOfferings } from '../../api/offerings'
import {
  getFarmersPerCooperative,
  getGradeDistribution,
  getOfferingCapacity,
  getOutstandingContributions,
} from '../../api/reports'
import { listUsers } from '../../api/users'
import { useAuth } from '../../auth/useAuth'
import { Badge } from '../../components/ui/Badge'
import { ROLE_LABELS } from '../../types/auth'
import { formatAmount, formatCount, formatDate, formatDateTime, formatPercent } from '../../utils/format'
import { FarmerLink } from '../admin/AccountBadges'
import { humanize } from '../admin/accountRules'
import { CardList, DashboardCard, Figure, Figures } from './DashboardCard'
import { OpenSeasonsCard } from './OpenSeasonsCard'
import { NEWEST_OFFERINGS, OFFERING_LIMIT, useResultCounts } from './dashboardQueries'

/** Offerings with results of one status waiting, and their total (decision D1). */
function WaitingResults({ title, offerings, status, linkLabel, empty, what }) {
  const counts = useResultCounts(offerings.data?.content, status)
  const query = offerings.error || offerings.isPending ? offerings : counts
  return (
    <DashboardCard title={title} query={query}>
      {(rows) => {
        const waiting = rows.filter((r) => r.count > 0)
        const total = waiting.reduce((sum, r) => sum + r.count, 0)
        const more = offerings.data.totalElements > OFFERING_LIMIT
        return (
          <div className="space-y-3">
            <Figure label={what} value={formatCount(total)} />
            <CardList
              empty={empty}
              rows={waiting.map(({ offering: o, count }) => ({
                key: o.id,
                main: `${o.programmeName} · ${o.seasonName}`,
                detail: o.districtName,
                to: `/offerings/${o.id}/results?status=${status}`,
                side: <span className="font-semibold text-ink">{count}</span>,
              }))}
            />
            {waiting.length > 0 && <p className="text-xs text-muted">{linkLabel}</p>}
            {more && (
              <p className="text-xs text-muted">
                Only the {OFFERING_LIMIT} newest of {offerings.data.totalElements} offerings are checked.
              </p>
            )}
          </div>
        )
      }}
    </DashboardCard>
  )
}

/** Field Officer: own offerings with places, results still in draft, permit verification. */
export function FieldOfficerDashboard() {
  const offerings = useQuery({
    queryKey: ['offerings', 'mine', 'dashboard'],
    queryFn: () => listMyOfferings(NEWEST_OFFERINGS),
  })
  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
      <DashboardCard title="My offerings" query={offerings} to="/offerings/mine" linkLabel="Open my offerings">
        {(page) => (
          <CardList
            empty="No offering is assigned to you."
            rows={page.content.slice(0, 6).map((o) => ({
              key: o.id,
              main: `${o.programmeName} · ${o.seasonName}`,
              to: `/offerings/${o.id}`,
              side: (
                <span className="text-sm">
                  <span className="font-semibold text-ink">
                    {o.enrolledCount} / {o.maxParticipants}
                  </span>
                  <span className="block text-xs text-muted">enrolled</span>
                </span>
              ),
            }))}
          />
        )}
      </DashboardCard>
      <WaitingResults
        title="Results still in draft"
        offerings={offerings}
        status="DRAFT"
        what="draft results to finish and submit"
        empty="No draft results: every result is submitted."
        linkLabel="Open an offering to enter scores and submit."
      />
      <DashboardCard title="Collection permits" to="/permits/verify" linkLabel="Verify a permit">
        <p className="text-sm text-muted">
          At input distribution, check a farmer’s permit number before handing over inputs.
        </p>
      </DashboardCard>
    </div>
  )
}

/** District Agronomist: results waiting for approval in the district, and the district's grades. */
export function AgronomistDashboard() {
  const { user } = useAuth()
  const offerings = useQuery({
    queryKey: ['offerings', 'district', user.districtId, 'dashboard'],
    queryFn: () => listOfferings(NEWEST_OFFERINGS, { districtId: user.districtId }),
  })
  const grades = useQuery({
    queryKey: ['reports', 'grade-distribution', undefined, user.districtId],
    queryFn: () => getGradeDistribution({ districtId: user.districtId }),
  })
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <WaitingResults
        title="Waiting for your approval"
        offerings={offerings}
        status="SUBMITTED"
        what="submitted results to approve or return"
        empty="Nothing is waiting for approval."
        linkLabel="Open an offering's results to approve or return them."
      />
      <DashboardCard title="Grades in your district" query={grades} to="/reports/grades" linkLabel="Open the report">
        {({ totals }) => (
          <div className="space-y-3">
            <Figures>
              <Figure label="Published results" value={formatCount(totals.published)} />
              <Figure label="Not yet published" value={formatCount(totals.notYetPublished)} />
              <Figure label="Average final score" value={formatAmount(totals.averageFinalScore)} />
            </Figures>
            {totals.published > 0 && (
              <p className="text-sm text-muted">
                {totals.grades.map((g) => `${g.grade} ${formatPercent(g.percent)}`).join(' · ')}
              </p>
            )}
          </div>
        )}
      </DashboardCard>
    </div>
  )
}

/** Union Registrar: open windows and the Union's headline figures from the reports. */
export function RegistrarDashboard() {
  const farmers = useQuery({
    queryKey: ['reports', 'farmers-per-cooperative', undefined],
    queryFn: () => getFarmersPerCooperative({}),
  })
  const capacity = useQuery({
    queryKey: ['reports', 'offering-capacity', undefined, undefined],
    queryFn: () => getOfferingCapacity({}),
  })
  const money = useQuery({
    queryKey: ['reports', 'outstanding-contributions', undefined, undefined],
    queryFn: () => getOutstandingContributions({}),
  })
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <OpenSeasonsCard />
      <DashboardCard title="Farmers" query={farmers} to="/reports/farmers" linkLabel="Open the report">
        {({ totals: t }) => (
          <Figures>
            <Figure label="Farmers" value={formatCount(t.total)} sub={`${formatCount(t.cooperatives)} cooperatives`} />
            <Figure label="Active" value={formatCount(t.active)} />
            <Figure label="Suspended" value={formatCount(t.suspended)} />
            <Figure label="Withdrawn" value={formatCount(t.withdrawn)} />
            <Figure label="Retired" value={formatCount(t.retired)} />
          </Figures>
        )}
      </DashboardCard>
      <DashboardCard title="Offering capacity" query={capacity} to="/reports/capacity" linkLabel="Open the report">
        {({ totals: t }) => (
          <Figures>
            <Figure label="Capacity used" value={formatPercent(t.capacityUsedPercent)} />
            <Figure
              label="Enrolled"
              value={formatCount(t.enrolled)}
              sub={`of ${formatCount(t.maxParticipants)} places`}
            />
            <Figure
              label="Full offerings"
              value={formatCount(t.fullOfferings)}
              sub={`of ${formatCount(t.offerings)}`}
            />
          </Figures>
        )}
      </DashboardCard>
      <OutstandingCard query={money} />
    </div>
  )
}

function OutstandingCard({ query }) {
  return (
    <DashboardCard title="Contributions" query={query} to="/reports/contributions" linkLabel="Open the report">
      {({ totals: t }) => (
        <Figures>
          <Figure label="Outstanding (RWF)" value={formatAmount(t.outstanding)} />
          <Figure label="Collected" value={formatPercent(t.collectedPercent)} />
          <Figure
            label="Pending payments (RWF)"
            value={formatAmount(t.pendingPayments)}
            sub="not counted as paid yet"
          />
        </Figures>
      )}
    </DashboardCard>
  )
}

/** Finance Officer: what is still owed, per season and district, and the latest contributions. */
export function FinanceDashboard() {
  const money = useQuery({
    queryKey: ['reports', 'outstanding-contributions', undefined, undefined],
    queryFn: () => getOutstandingContributions({}),
  })
  const recent = useQuery({
    queryKey: ['contributions', 'dashboard'],
    queryFn: () => listContributions({ page: 0, size: 5, sortBy: 'createdAt', direction: 'desc' }),
  })
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <OutstandingCard query={money} />
      <DashboardCard
        title="Outstanding per season"
        query={money}
        to="/reports/contributions"
        linkLabel="Open the report"
      >
        {({ rows }) => (
          <CardList
            empty="No contribution has been set yet."
            rows={rows.map((r) => ({
              key: `${r.seasonId}-${r.districtId}`,
              main: `${r.seasonName} · ${r.districtName}`,
              detail: `${formatPercent(r.collectedPercent)} collected`,
              side: <span className="font-semibold text-ink">{formatAmount(r.outstanding)} RWF</span>,
            }))}
          />
        )}
      </DashboardCard>
      <DashboardCard
        title="Latest contributions"
        query={recent}
        to="/contributions"
        linkLabel="Open contributions"
        wide
      >
        {(page) => (
          <CardList
            empty="No contribution has been set yet."
            rows={page.content.map((c) => ({
              key: c.id,
              main: c.farmerName,
              detail: `${c.membershipNumber} · ${c.seasonName}`,
              to: `/contributions/${c.id}`,
              side: (
                <span className="text-sm">
                  <span className="font-semibold text-ink">{formatAmount(c.outstanding)} RWF</span>
                  <span className="block text-xs text-muted">outstanding · {c.paidPercent}% paid</span>
                </span>
              ),
            }))}
          />
        )}
      </DashboardCard>
    </div>
  )
}

/** System Administrator: number of accounts and the newest ones (decision D3), and the latest audit events. */
export function AdminDashboard() {
  const users = useQuery({
    queryKey: ['users', 'dashboard'],
    queryFn: () => listUsers({ page: 0, size: 5, sortBy: 'createdAt', direction: 'desc' }),
  })
  const events = useQuery({
    queryKey: ['audit-logs', 'dashboard'],
    queryFn: () => listAuditLogs({ page: 0, size: 8, sortBy: 'occurredAt', direction: 'desc' }),
  })
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <DashboardCard title="Accounts" query={users} to="/admin/users" linkLabel="Open users">
        {(page) => (
          <div className="space-y-3">
            <Figure label="accounts in total" value={formatCount(page.totalElements)} />
            <CardList
              empty="There are no accounts."
              rows={page.content.map((u) => ({
                key: u.id,
                main: u.username,
                detail: `${ROLE_LABELS[u.role] ?? u.role} · created ${formatDate(u.createdAt)}`,
                to: `/admin/users/${u.id}`,
                side:
                  u.role === 'FARMER' && u.farmerId == null ? (
                    <FarmerLink user={u} />
                  ) : (
                    !u.enabled && <Badge tone="danger">Disabled</Badge>
                  ),
              }))}
            />
          </div>
        )}
      </DashboardCard>
      <DashboardCard title="Latest audit events" query={events} to="/admin/audit-log" linkLabel="Open the audit log">
        {(page) => (
          <CardList
            empty="The audit log is empty."
            rows={page.content.map((e) => ({
              key: e.id,
              main: humanize(e.eventType),
              detail: `${formatDateTime(e.occurredAt)} · ${e.actorUsername ?? 'unknown'}`,
              side: e.success ? <Badge tone="success">Succeeded</Badge> : <Badge tone="danger">Failed</Badge>,
            }))}
          />
        )}
      </DashboardCard>
    </div>
  )
}
