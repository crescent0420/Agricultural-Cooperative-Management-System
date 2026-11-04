import { useId, type ComponentProps, type ReactNode } from 'react'

interface TextFieldProps extends ComponentProps<'input'> {
  label: string
  /** Error under the field; also marks the input invalid for screen readers. */
  error?: string
  hint?: string
  /** Something inside the right edge of the input, e.g. a show-password button. */
  trailing?: ReactNode
}

/** A labelled input. Works with React Hook Form's register(), which passes ref through props (React 19). */
export function TextField({ label, error, hint, trailing, id, className = '', ...inputProps }: TextFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const hintId = hint ? `${inputId}-hint` : undefined
  const errorId = error ? `${inputId}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined

  return (
    <div className={className}>
      <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-ink">
        {label}
      </label>
      <div className="relative">
        <input
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink outline-none placeholder:text-muted focus:border-brand-600 focus:ring-2 focus:ring-brand-200 ${error ? 'border-danger' : 'border-line'} ${trailing ? 'pr-20' : ''}`}
          {...inputProps}
        />
        {trailing && <div className="absolute inset-y-0 right-2 flex items-center">{trailing}</div>}
      </div>
      {hint && (
        <p id={hintId} className="mt-1 text-xs text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="mt-1 text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
