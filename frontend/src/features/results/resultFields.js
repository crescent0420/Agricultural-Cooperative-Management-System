/** The fields the result lists can sort by (ResultService.SORTABLE_FIELDS). */
export const RESULT_SORTABLE = ['id', 'createdAt', 'finalScore', 'status']

export const RESULT_STATUS_LABELS = { DRAFT: 'Draft', SUBMITTED: 'Submitted', PUBLISHED: 'Published' }

/** A score as the API sends it, or a dash when there is none. */
export const scoreText = (value) => (value == null ? '—' : String(value))

/** ScoreEntry.score: @DecimalMin("0.00") @Digits(4, 2), and at most the inspection's maximum. */
export function scoreRules(maxScore, { required = false } = {}) {
  return {
    validate: (v) => {
      if (v === '' || v == null) return required ? 'Enter a score.' : true
      if (!/^\d{1,4}(\.\d{1,2})?$/.test(String(v))) return 'Use a number with at most 2 decimals.'
      return Number(v) <= Number(maxScore) || `The score must be between 0 and ${maxScore}.`
    },
  }
}
