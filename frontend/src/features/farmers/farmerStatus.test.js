import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { FARMER_STATUSES, NEXT_STATUSES } from './farmerStatus'

// The backend's rule, read from the Java source in the same repository. Tests run from ACMS/frontend,
// so the backend is one folder up: ACMS/src/main/java/...
const javaSource = readFileSync(join(process.cwd(), '..', 'src/main/java/com/acms/enums/FarmerStatus.java'), 'utf8')

/** Turns "case ACTIVE -> target == SUSPENDED || target == WITHDRAWN ..." into { ACTIVE: ['SUSPENDED', ...] }. */
function backendTransitions(source) {
  const rule = source.slice(source.indexOf('canChangeTo'))
  const table = {}
  for (const match of rule.matchAll(/case\s+(\w+)\s*->([^;]*);/g)) {
    table[match[1]] = [...match[2].matchAll(/target\s*==\s*(\w+)/g)].map((m) => m[1])
  }
  return table
}

describe('farmer statuses', () => {
  it('lists the same four statuses as the backend enum', () => {
    const declared = javaSource.match(/enum FarmerStatus\s*\{([^;]*);/)[1]
    expect(declared.split(',').map((s) => s.trim())).toEqual(FARMER_STATUSES)
  })

  it('offers exactly the status changes FarmerStatus.canChangeTo allows (URD FR-FAR-05)', () => {
    const backend = backendTransitions(javaSource)
    expect(Object.keys(backend).sort()).toEqual([...FARMER_STATUSES].sort())
    for (const status of FARMER_STATUSES) {
      expect([...NEXT_STATUSES[status]].sort(), status).toEqual([...backend[status]].sort())
    }
    expect(NEXT_STATUSES.RETIRED).toEqual([])
  })
})
