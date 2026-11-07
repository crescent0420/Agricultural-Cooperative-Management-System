import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { getOfferingCapacity } from '../../api/reports'
import { useAuth } from '../../auth/useAuth'
import { Badge } from '../../components/ui/Badge'
import { formatAmount, formatCount, formatPercent } from '../../utils/format'
import { CHART_LIMIT, limitNote, REMAINDER, SERIES } from './chartColors'
import { StackedBars } from './ReportCharts'
import { ReportTable, ReportView } from './ReportView'

const SEATS = [
  { key: 'enrolled', label: 'Enrolled', color: SERIES.blue },
  { key: 'placesLeft', label: 'Places left', color: REMAINDER },
]

/** FR-REP-02: enrolments and capacity per programme offering. */
export function CapacityReport({ filters, names }) {
  const { hasPermission } = useAuth()
  // The same permissions as the /offerings/:id route.
  const canOpen = hasPermission('OFFERING_READ') && hasPermission('ENROLMENT_LIST_FOR_OFFERING')
  const query = useQuery({
    queryKey: ['reports', 'offering-capacity', filters.seasonId, filters.districtId],
    queryFn: () => getOfferingCapacity(filters),
  })
  const name = (r) => `${r.programmeName} · ${r.districtName} · ${r.seasonName}`

  return (
    <ReportView
      query={query}
      names={{ ...names, withSeason: true }}
      emptyMessage="No programme offerings match these filters."
      tiles={(t) => [
        { label: 'Offerings', value: formatCount(t.offerings) },
        { label: 'Full offerings', value: formatCount(t.fullOfferings) },
        { label: 'Enrolled', value: formatCount(t.enrolled), sub: `of ${formatCount(t.maxParticipants)} places` },
        { label: 'Capacity used', value: formatPercent(t.capacityUsedPercent) },
        { label: 'Places left', value: formatCount(t.placesLeft), sub: `${formatCount(t.cancelled)} cancelled` },
        { label: 'Hectares enrolled', value: formatAmount(t.hectaresEnrolled) },
      ]}
      chart={({ rows }) => (
        <StackedBars
          label="Enrolled and places left per offering"
          data={rows.slice(0, CHART_LIMIT).map((r) => ({
            ...r,
            label: name(r),
            fullLabel: name(r),
            detail: `${r.fieldOfficerUsername ?? 'No field officer'} · ${formatPercent(r.capacityUsedPercent)} used`,
          }))}
          series={SEATS}
          format={formatCount}
          total={{ label: 'Places', value: (r) => r.maxParticipants }}
          note={limitNote(rows.length, 'offerings (newest season first)')}
        />
      )}
      table={({ rows, totals }) => (
        <ReportTable
          caption="Enrolments and capacity per offering"
          rows={rows}
          rowKey={(r) => r.offeringId}
          totals={totals}
          columns={[
            {
              header: 'Offering',
              cell: (r) =>
                canOpen ? (
                  <Link to={`/offerings/${r.offeringId}`} className="font-medium text-brand-700 hover:underline">
                    {r.programmeName}
                  </Link>
                ) : (
                  r.programmeName
                ),
            },
            { header: 'Season', cell: (r) => r.seasonName, total: (t) => `${formatCount(t.offerings)} offerings` },
            { header: 'District', cell: (r) => r.districtName },
            { header: 'Field officer', cell: (r) => r.fieldOfficerUsername ?? '—' },
            {
              header: 'Places',
              numeric: true,
              cell: (r) => formatCount(r.maxParticipants),
              total: (t) => formatCount(t.maxParticipants),
            },
            {
              header: 'Enrolled',
              numeric: true,
              cell: (r) => formatCount(r.enrolled),
              total: (t) => formatCount(t.enrolled),
            },
            {
              header: 'Cancelled',
              numeric: true,
              cell: (r) => formatCount(r.cancelled),
              total: (t) => formatCount(t.cancelled),
            },
            {
              header: 'Places left',
              numeric: true,
              cell: (r) => formatCount(r.placesLeft),
              total: (t) => formatCount(t.placesLeft),
            },
            {
              header: 'Used',
              numeric: true,
              cell: (r) => (
                <span className="inline-flex items-center gap-2">
                  {r.full && <Badge>Full</Badge>}
                  {formatPercent(r.capacityUsedPercent)}
                </span>
              ),
              total: (t) => formatPercent(t.capacityUsedPercent),
            },
            {
              header: 'Hectares',
              numeric: true,
              cell: (r) => formatAmount(r.hectaresEnrolled),
              total: (t) => formatAmount(t.hectaresEnrolled),
            },
          ]}
        />
      )}
    />
  )
}
