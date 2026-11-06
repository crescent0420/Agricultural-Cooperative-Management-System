import { useId } from 'react'
import { describedBy } from './describedBy'
import { FieldMessages } from './FieldMessages'

/** A labelled dropdown. Works with React Hook Form's register() like TextField. */
export function Select({ label, error, warning, hint, id, className = '', children, ...selectProps }) {
  const generatedId = useId()
  const selectId = id ?? generatedId

  return (
    <div className={className}>
      <label htmlFor={selectId} className="mb-1 block text-sm font-medium text-ink">
        {label}
      </label>
      <select
        id={selectId}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(selectId, { hint, warning, error })}
        className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-200 disabled:bg-surface disabled:text-muted ${error ? 'border-danger' : 'border-line'}`}
        {...selectProps}
      >
        {children}
      </select>
      <FieldMessages id={selectId} hint={hint} warning={warning} error={error} />
    </div>
  )
}
