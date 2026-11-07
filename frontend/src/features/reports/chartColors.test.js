import { describe, expect, it } from 'vitest'
import { formatCount, formatPercent } from '../../utils/format'
import { CHART_LIMIT, limitNote } from './chartColors'

describe('report helpers', () => {
  it('formats counts and the percentages the backend already rounded', () => {
    expect(formatCount(1234)).toBe('1,234')
    expect(formatPercent(74.29)).toBe('74.29%')
    expect(formatPercent(100)).toBe('100%')
    expect(formatPercent(null)).toBe('—')
  })

  it('says when the chart shows only part of the rows', () => {
    expect(limitNote(CHART_LIMIT, 'cooperatives')).toBeUndefined()
    expect(limitNote(40, 'cooperatives')).toBe(
      'The chart shows 15 of 40 cooperatives; the table below lists all of them.',
    )
  })
})
