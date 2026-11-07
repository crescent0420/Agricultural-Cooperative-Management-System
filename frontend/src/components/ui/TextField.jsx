import { useId } from 'react'
import { describedBy } from './describedBy'
import { FieldMessages } from './FieldMessages'
import { useBlockedAttempt } from './useBlockedAttempt'

/**
 * A labelled input. Works with React Hook Form's register(), which passes ref through props (React 19).
 *
 * @param {object} props  Any <input> attribute, plus:
 * @param {string} props.label
 * @param {string} [props.error]  Error under the field; also marks the input invalid for screen readers.
 * @param {string} [props.hint]  Grey information under the field.
 * @param {string} [props.warning]  Yellow: something to look at before continuing (the field still works).
 * @param {string} [props.blockedReason]  The field cannot be changed now: it is read-only, and this reason appears
 *   as a yellow warning only when the user tries to change it.
 * @param {import('react').ReactNode} [props.trailing]  Something inside the right edge, e.g. a show-password button.
 */
export function TextField({ label, error, warning, hint, blockedReason, trailing, id, className = '', ...inputProps }) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const attempt = useBlockedAttempt(blockedReason)
  const shownWarning = attempt.message ?? warning

  return (
    <div className={className}>
      <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-ink">
        {label}
      </label>
      <div className="relative">
        <input
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(inputId, { hint, warning: shownWarning, error })}
          aria-disabled={attempt.blocked || undefined}
          className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink outline-none placeholder:text-muted focus:border-brand-600 focus:ring-2 focus:ring-brand-200 aria-disabled:cursor-not-allowed aria-disabled:bg-surface aria-disabled:text-muted ${error ? 'border-danger' : 'border-line'} ${trailing ? 'pr-20' : ''}`}
          {...inputProps}
          readOnly={attempt.blocked || inputProps.readOnly}
          {...attempt.handlers}
        />
        {trailing && <div className="absolute inset-y-0 right-2 flex items-center">{trailing}</div>}
      </div>
      <FieldMessages id={inputId} hint={hint} warning={shownWarning} error={error} />
    </div>
  )
}
