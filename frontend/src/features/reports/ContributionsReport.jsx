import { useQuery } from '@tanstack/react-query'
import { getOutstandingContributions } from '../../api/reports'
import { formatAmount, formatCount, formatPercent } from '../../utils/format'
import { CHART_LIMIT, limitNote, SERIES } from './chartColors'
import { StackedBars } from './ReportCharts'
import { ReportTable, ReportView } from './ReportView'

// Paid + outstanding = amount due. Pending payments are part of "outstanding" until the Finance Officer confirms them.
const MONEY = [
  { key: 'confirmedPaid', label: 'Confirmed paid', color: SERIES.blue },
  { key: 'outstanding', label: 'Outstanding', color: SERIES.orange },
]
const rwf = (v) => `${formatAmount(v)} RWF`

/** FR-REP-03: outstanding contributions per season and district. */
export function ContributionsReport({ filters, names }) {
  const query = useQuery({
    queryKey: ['reports', 'outstanding-contributions', filters.seasonId, filters.districtId],
    queryFn: () => getOutstandingContributions(filters),
  })
  const name = (r) => `${r.seasonName} · ${r.districtName}`

  return (
    <ReportView
      query={query}
      names={{ ...names, withSeason: true }}
      emptyMessage="No contributions match these filters."
      tiles={(t) => [
        {
          label: 'Amount due (RWF)',
          value: formatAmount(t.amountDue),
          sub: `${formatCount(t.contributions)} contributions`,
        },
        { label: 'Confirmed paid (RWF)', value: formatAmount(t.confirmedPaid) },
        { label: 'Outstanding (RWF)', value: formatAmount(t.outstanding) },
        { label: 'Collected', value: formatPercent(t.collectedPercent) },
        { label: 'Pending payments (RWF)', value: formatAmount(t.pendingPayments), sub: 'not counted as paid yet' },
        {
          label: 'Fully paid',
          value: formatCount(t.fullyPaid),
          sub: `${formatCount(t.partlyPaid)} partly · ${formatCount(t.unpaid)} unpaid`,
        },
      ]}
      chart={({ rows }) => (
        <StackedBars
          label="Confirmed paid and outstanding (RWF) per season and district"
          data={rows.slice(0, CHART_LIMIT).map((r) => ({
            ...r,
            confirmedPaid: Number(r.confirmedPaid),
            outstanding: Number(r.outstanding),
            label: name(r),
            fullLabel: name(r),
            detail: `${formatPercent(r.collectedPercent)} collected · ${rwf(r.pendingPayments)} pending`,
          }))}
          series={MONEY}
          format={formatAmount}
          total={{ label: 'Amount due', value: (r) => r.amountDue }}
          note={limitNote(rows.length, 'season and district rows')}
        />
      )}
      table={({ rows, totals }) => (
        <ReportTable
          caption="Outstanding contributions per season and district"
          rows={rows}
          rowKey={(r) => `${r.seasonId}-${r.districtId}`}
          totals={totals}
          columns={[
            { header: 'Season', cell: (r) => r.seasonName },
            { header: 'District', cell: (r) => r.districtName },
            {
              header: 'Contributions',
              numeric: true,
              cell: (r) => formatCount(r.contributions),
              total: (t) => formatCount(t.contributions),
            },
            {
              header: 'Due (RWF)',
              numeric: true,
              cell: (r) => formatAmount(r.amountDue),
              total: (t) => formatAmount(t.amountDue),
            },
            {
              header: 'Confirmed paid',
              numeric: true,
              cell: (r) => formatAmount(r.confirmedPaid),
              total: (t) => formatAmount(t.confirmedPaid),
            },
            {
              header: 'Pending',
              numeric: true,
              cell: (r) => formatAmount(r.pendingPayments),
              total: (t) => formatAmount(t.pendingPayments),
            },
            {
              header: 'Outstanding',
              numeric: true,
              cell: (r) => formatAmount(r.outstanding),
              total: (t) => formatAmount(t.outstanding),
            },
            {
              header: 'Collected',
              numeric: true,
              cell: (r) => formatPercent(r.collectedPercent),
              total: (t) => formatPercent(t.collectedPercent),
            },
            {
              header: 'Fully paid',
              numeric: true,
              cell: (r) => formatCount(r.fullyPaid),
              total: (t) => formatCount(t.fullyPaid),
            },
            {
              header: 'Partly paid',
              numeric: true,
              cell: (r) => formatCount(r.partlyPaid),
              total: (t) => formatCount(t.partlyPaid),
            },
            {
              header: 'Unpaid',
              numeric: true,
              cell: (r) => formatCount(r.unpaid),
              total: (t) => formatCount(t.unpaid),
            },
          ]}
        />
      )}
    />
  )
}
