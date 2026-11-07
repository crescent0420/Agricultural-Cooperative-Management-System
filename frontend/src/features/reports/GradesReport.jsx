import { useQuery } from '@tanstack/react-query'
import { getGradeDistribution } from '../../api/reports'
import { formatAmount, formatCount, formatPercent } from '../../utils/format'
import { SERIES } from './chartColors'
import { GradeColumns } from './ReportCharts'
import { ReportTable, ReportView } from './ReportView'

const gradeCell = (g) => (g ? `${formatCount(g.count)} (${formatPercent(g.percent)})` : '—')

/** FR-REP-04: grade distribution per programme offering. Only published results count. */
export function GradesReport({ filters, names }) {
  const query = useQuery({
    queryKey: ['reports', 'grade-distribution', filters.seasonId, filters.districtId],
    queryFn: () => getGradeDistribution(filters),
  })

  return (
    <ReportView
      query={query}
      names={{ ...names, withSeason: true }}
      emptyMessage="No programme offerings match these filters."
      tiles={(t) => [
        { label: 'Offerings', value: formatCount(t.offerings) },
        { label: 'Published results', value: formatCount(t.published) },
        { label: 'Not yet published', value: formatCount(t.notYetPublished) },
        { label: 'Average final score', value: formatAmount(t.averageFinalScore) },
      ]}
      chart={({ totals }) =>
        totals.published > 0 ? (
          <GradeColumns
            label="Published results per grade"
            grades={totals.grades}
            color={SERIES.blue}
            formatPercent={formatPercent}
          />
        ) : (
          <p className="rounded-lg border border-line bg-white px-4 py-6 text-center text-sm text-muted">
            No results are published yet, so there is no grade distribution to show.
          </p>
        )
      }
      table={({ rows, totals }) => {
        // R-3: one column per grade of the totals (the current scale, best first, then any older grade).
        const grades = totals.grades.map((g) => g.grade)
        const find = (list, grade) => list.find((g) => g.grade === grade)
        return (
          <ReportTable
            caption="Grade distribution per offering"
            rows={rows}
            rowKey={(r) => r.offeringId}
            totals={totals}
            columns={[
              { header: 'Offering', cell: (r) => r.programmeName },
              { header: 'Season', cell: (r) => r.seasonName, total: (t) => `${formatCount(t.offerings)} offerings` },
              { header: 'District', cell: (r) => r.districtName },
              { header: 'Field officer', cell: (r) => r.fieldOfficerUsername ?? '—' },
              { header: 'Active enrolments', numeric: true, cell: (r) => formatCount(r.activeEnrolments) },
              {
                header: 'Published',
                numeric: true,
                cell: (r) => formatCount(r.published),
                total: (t) => formatCount(t.published),
              },
              {
                header: 'Not yet published',
                numeric: true,
                cell: (r) => formatCount(r.notYetPublished),
                total: (t) => formatCount(t.notYetPublished),
              },
              {
                header: 'Average score',
                numeric: true,
                cell: (r) => formatAmount(r.averageFinalScore),
                total: (t) => formatAmount(t.averageFinalScore),
              },
              ...grades.map((grade) => ({
                header: `Grade ${grade}`,
                numeric: true,
                cell: (r) => gradeCell(find(r.grades, grade)),
                total: (t) => gradeCell(find(t.grades, grade)),
              })),
            ]}
          />
        )
      }}
    />
  )
}
