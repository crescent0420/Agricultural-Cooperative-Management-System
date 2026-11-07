import { useId } from 'react'
import { describedBy } from './describedBy'
import { FieldMessages } from './FieldMessages'
import { useBlockedAttempt } from './useBlockedAttempt'

/**
 * A labelled dropdown. Works with React Hook Form's register() like TextField.
 * blockedReason: the dropdown cannot be used yet; it looks unavailable, and the reason appears as a yellow
 * warning only when the user tries to open it (project rule: warnings on attempt, not before).
 */
export function Select({ label, error, warning, hint, blockedReason, id, className = '', children, ...selectProps }) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  const attempt = useBlockedAttempt(blockedReason)
  const shownWarning = attempt.message ?? warning

  return (
    <div className={className}>
      <label htmlFor={selectId} className="mb-1 block text-sm font-medium text-ink">
        {label}
      </label>
      <select
        id={selectId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(selectId, { hint, warning: shownWarning, error })}
        aria-disabled={attempt.blocked || undefined}
        className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200 disabled:bg-surface disabled:text-muted aria-disabled:cursor-not-allowed aria-disabled:bg-surface aria-disabled:text-muted ${error ? 'border-danger' : 'border-line'}`}
        {...selectProps}
        {...attempt.handlers}
      >
        {children}
      </select>
      <FieldMessages id={selectId} hint={hint} warning={shownWarning} error={error} />
    </div>
  )
}
