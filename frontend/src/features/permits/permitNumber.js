// Permit numbers: CP-XXXX-XXXX-XXXX, from the 31 characters PermitService uses (no I, L, O, 0 or 1, which
// are easy to confuse on paper). Decision T2: checked here before sending (URD FR-PER-03).

const GROUP = '[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}'
const PERMIT_NUMBER = new RegExp(`^CP-${GROUP}-${GROUP}-${GROUP}$`)

/** Upper case, no spaces; dashes added when the 14 characters were typed without them. */
export function normalizePermitNumber(input) {
  const compact = input.toUpperCase().replace(/\s+/g, '')
  const bare = compact.replace(/-/g, '')
  if (/^CP[A-Z0-9]{12}$/.test(bare)) {
    return `CP-${bare.slice(2, 6)}-${bare.slice(6, 10)}-${bare.slice(10, 14)}`
  }
  return compact
}

/** null when the number is well formed, otherwise what is wrong with it. */
export function permitNumberProblem(normalized) {
  if (normalized === '') return 'Enter the permit number.'
  if (PERMIT_NUMBER.test(normalized)) return null
  if (/[ILO01]/.test(normalized.replace(/^CP-/, ''))) {
    return 'Permit numbers never contain I, L, O, 0 or 1, as they are easy to confuse. Check the number on the permit again.'
  }
  return 'A permit number looks like CP-ABCD-EFGH-JKMN: CP, then three groups of four letters or digits.'
}
