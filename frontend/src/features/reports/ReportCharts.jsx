import { Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CHROME } from './chartColors'

const ROW_HEIGHT = 34
const shorten = (text, max) => (text.length > max ? `${text.slice(0, max - 1)}…` : text)

/** A bar's name on one line, shortened with "…"; the full name shows on hover. */
function CategoryTick({ x, y, payload, maxChars }) {
  const text = String(payload?.value ?? '')
  return (
    <text x={x} y={y} dy={4} textAnchor="end" fill={CHROME.tick} fontSize={12}>
      <title>{text}</title>
      {shorten(text, maxChars)}
    </text>
  )
}

function Swatch({ color }) {
  return <span aria-hidden="true" className="inline-block size-2.5 shrink-0 rounded-sm" style={{ background: color }} />
}

/** The legend, in HTML above the chart: names in the text colour, a coloured square beside each. */
export function ChartLegend({ series }) {
  return (
    <ul className="mb-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted" aria-label="Legend">
      {series.map((s) => (
        <li key={s.key} className="flex items-center gap-1.5">
          <Swatch color={s.color} />
          {s.label}
        </li>
      ))}
    </ul>
  )
}

function ChartTooltip({ active, payload, series, format, total }) {
  if (!active || !payload?.length) return null
  const row = payload[0].payload
  return (
    <div className="rounded-md border border-line bg-white px-3 py-2 text-xs shadow-md">
      <p className="mb-1 max-w-64 font-semibold text-ink">{row.fullLabel}</p>
      {row.detail && <p className="mb-1 text-muted">{row.detail}</p>}
      <ul className="space-y-0.5">
        {series.map((s) => (
          <li key={s.key} className="flex items-center gap-2 text-muted">
            <Swatch color={s.color} />
            {s.label}
            <span className="ml-auto pl-3 font-medium text-ink tabular-nums">{format(row[s.key])}</span>
          </li>
        ))}
        {total && (
          <li className="flex gap-2 border-t border-line pt-0.5 text-muted">
            {total.label}
            <span className="ml-auto pl-3 font-medium text-ink tabular-nums">
              {(total.format ?? format)(total.value(row))}
            </span>
          </li>
        )}
      </ul>
    </div>
  )
}

/**
 * Horizontal stacked bars, one per row (cooperative, offering, season and district). Bars are at most 20 px
 * thick, separated by a 2 px white gap, rounded only at the free end. At most CHART_LIMIT bars; the caller
 * says which ones and the note says so.
 * @param {{ label: string, data: object[], series: { key: string, label: string, color: string }[],
 *   format: (v: number) => string, total?: { label: string, value: (row: object) => number, format?: (v: number) => string }, note?: string }} props
 *   data items: { id, label, fullLabel, detail?, [series.key]: number }
 */
export function StackedBars({ label, data, series, format, total, note }) {
  const height = data.length * ROW_HEIGHT + 40
  // Narrow screens get shorter names so the bars keep room (the full name is in the tooltip and the table).
  const wide = typeof window === 'undefined' || window.innerWidth >= 640
  const labelWidth = wide ? 210 : 110
  const maxChars = wide ? 32 : 15
  const last = series.length - 1
  return (
    <figure className="rounded-lg border border-line bg-white p-4">
      <figcaption className="mb-2 text-sm font-semibold text-ink">{label}</figcaption>
      <ChartLegend series={series} />
      <div style={{ height }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 4 }} barSize={20}>
            <CartesianGrid horizontal={false} stroke={CHROME.grid} />
            <XAxis
              type="number"
              allowDecimals={false}
              tickFormatter={format}
              tick={{ fill: CHROME.tick, fontSize: 12 }}
              stroke={CHROME.axis}
            />
            <YAxis
              type="category"
              dataKey="label"
              width={labelWidth}
              interval={0}
              tick={<CategoryTick maxChars={maxChars} />}
              stroke={CHROME.axis}
            />
            <Tooltip
              cursor={{ fill: 'rgba(11,11,11,0.04)' }}
              content={(props) => <ChartTooltip {...props} series={series} format={format} total={total} />}
            />
            {series.map((s, i) => (
              <Bar
                key={s.key}
                dataKey={s.key}
                name={s.label}
                stackId="one"
                fill={s.color}
                stroke="#ffffff"
                strokeWidth={1}
                radius={i === last ? [0, 4, 4, 0] : 0}
                isAnimationActive={false}
              />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
      {note && <p className="mt-2 text-xs text-muted">{note}</p>}
    </figure>
  )
}

/**
 * Columns, one per grade, best grade first, with the count written on top (few bars, so each gets its label).
 * A single series: the title names it, no legend.
 * @param {{ label: string, grades: { grade: string, count: number, percent: number | null }[], color: string,
 *   formatPercent: (v: number | null) => string }} props
 */
export function GradeColumns({ label, grades, color, formatPercent }) {
  const data = grades.map((g) => ({ ...g, fullLabel: `Grade ${g.grade}`, count: Number(g.count) }))
  return (
    <figure className="rounded-lg border border-line bg-white p-4">
      <figcaption className="mb-2 text-sm font-semibold text-ink">{label}</figcaption>
      <div style={{ height: 260 }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 20, right: 8, bottom: 4, left: 0 }} barSize={24}>
            <CartesianGrid vertical={false} stroke={CHROME.grid} />
            <XAxis dataKey="grade" tick={{ fill: CHROME.tick, fontSize: 12 }} stroke={CHROME.axis} />
            <YAxis allowDecimals={false} tick={{ fill: CHROME.tick, fontSize: 12 }} stroke={CHROME.axis} width={40} />
            <Tooltip
              cursor={{ fill: 'rgba(11,11,11,0.04)' }}
              content={(props) => (
                <ChartTooltip
                  {...props}
                  series={[{ key: 'count', label: 'Published results', color }]}
                  format={(v) => String(v)}
                  total={{ label: 'Share', value: (row) => row.percent, format: formatPercent }}
                />
              )}
            />
            <Bar dataKey="count" name="Published results" fill={color} radius={[4, 4, 0, 0]} isAnimationActive={false}>
              <LabelList dataKey="count" position="top" style={{ fill: CHROME.tick, fontSize: 12 }} />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <p className="mt-2 text-xs text-muted">
        Shares: {grades.map((g) => `${g.grade} ${formatPercent(g.percent)}`).join(' · ')}
      </p>
    </figure>
  )
}
