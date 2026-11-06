import { useId } from 'react'
import { describedBy } from './describedBy'
import { FieldMessages } from './FieldMessages'

/** A labelled multi-line input, with the same hint (grey), warning (yellow) and error (red) lines as TextField. */
export function TextArea({ label, error, warning, hint, id, className = '', rows = 3, ...textareaProps }) {
  const generatedId = useId()
  const areaId = id ?? generatedId
  return (
    <div className={className}>
      <label htmlFor={areaId} className="mb-1 block text-sm font-medium text-ink">
        {label}
      </label>
      <textarea
        id={areaId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(areaId, { hint, warning, error })}
        className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink outline-none placeholder:text-muted focus:border-brand-600 focus:ring-2 focus:ring-brand-200 ${error ? 'border-danger' : 'border-line'}`}
        {...textareaProps}
      />
      <FieldMessages id={areaId} hint={hint} warning={warning} error={error} />
    </div>
  )
}
