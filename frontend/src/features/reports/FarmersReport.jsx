import { useQuery } from '@tanstack/react-query'
import { getFarmersPerCooperative } from '../../api/reports'
import { formatCount } from '../../utils/format'
import { CHART_LIMIT, limitNote, SERIES } from './chartColors'
import { StackedBars } from './ReportCharts'
import { ReportTable, ReportView } from './ReportView'

const STATUSES = [
  { key: 'active', label: 'Active', color: SERIES.blue },
  { key: 'suspended', label: 'Suspended', color: SERIES.orange },
  { key: 'withdrawn', label: 'Withdrawn', color: SERIES.aqua },
  { key: 'retired', label: 'Retired', color: SERIES.yellow },
]

/** FR-REP-01: farmers per cooperative and status. No season filter (farmers are counted by current status). */
export function FarmersReport({ filters, names }) {
  const query = useQuery({
    queryKey: ['reports', 'farmers-per-cooperative', filters.districtId],
    queryFn: () => getFarmersPerCooperative({ districtId: filters.districtId }),
  })

  return (
    <ReportView
      query={query}
      names={{ ...names, withSeason: false }}
      emptyMessage="There are no cooperatives in this district."
      tiles={(t) => [
        { label: 'Cooperatives', value: formatCount(t.cooperatives) },
        { label: 'Farmers', value: formatCount(t.total) },
        ...STATUSES.map((s) => ({ label: s.label, value: formatCount(t[s.key]) })),
      ]}
      chart={({ rows }) => {
        // The cooperatives with the most farmers; cooperatives without farmers stay in the table only.
        const top = rows
          .filter((r) => r.total > 0)
          .toSorted((a, b) => b.total - a.total || a.cooperativeName.localeCompare(b.cooperativeName))
          .slice(0, CHART_LIMIT)
          .map((r) => ({ ...r, label: r.cooperativeName, fullLabel: r.cooperativeName, detail: r.districtName }))
        if (top.length === 0) return null
        return (
          <StackedBars
            label="Farmers per cooperative, by status"
            data={top}
            series={STATUSES}
            format={formatCount}
            total={{ label: 'Total', value: (r) => r.total }}
            note={limitNote(rows.filter((r) => r.total > 0).length, 'cooperatives with farmers')}
          />
        )
      }}
      table={({ rows, totals }) => (
        <ReportTable
          caption="Farmers per cooperative"
          rows={rows}
          rowKey={(r) => r.cooperativeId}
          totals={totals}
          columns={[
            { header: 'Cooperative', cell: (r) => r.cooperativeName },
            {
              header: 'District',
              cell: (r) => r.districtName,
              total: (t) => `${formatCount(t.cooperatives)} cooperatives`,
            },
            ...STATUSES.map((s) => ({
              header: s.label,
              numeric: true,
              cell: (r) => formatCount(r[s.key]),
              total: (t) => formatCount(t[s.key]),
            })),
            { header: 'Total', numeric: true, cell: (r) => formatCount(r.total), total: (t) => formatCount(t.total) },
          ]}
        />
      )}
    />
  )
}
