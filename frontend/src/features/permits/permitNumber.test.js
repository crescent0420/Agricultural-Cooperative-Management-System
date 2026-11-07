import { describe, expect, it } from 'vitest'
import { normalizePermitNumber, permitNumberProblem } from './permitNumber'

describe('permit numbers (T2)', () => {
  it('normalizes what a storekeeper types', () => {
    expect(normalizePermitNumber(' cp-ab2c-d3ef-gh4j ')).toBe('CP-AB2C-D3EF-GH4J')
    expect(normalizePermitNumber('CPAB2CD3EFGH4J')).toBe('CP-AB2C-D3EF-GH4J')
    expect(normalizePermitNumber('cp ab2c d3ef gh4j')).toBe('CP-AB2C-D3EF-GH4J')
  })

  it('accepts only the backend’s format and alphabet', () => {
    expect(permitNumberProblem('CP-AB2C-D3EF-GH4J')).toBeNull()
    expect(permitNumberProblem('')).toBe('Enter the permit number.')
    expect(permitNumberProblem('CP-AB2C-D3EF')).toMatch(/looks like CP-ABCD-EFGH-JKMN/)
    expect(permitNumberProblem('CP-AB0C-D3EF-GH4J')).toMatch(/never contain I, L, O, 0 or 1/)
    expect(permitNumberProblem('CP-ABIC-D3EF-GH4J')).toMatch(/never contain/)
  })
})
