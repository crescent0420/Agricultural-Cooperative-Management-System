/**
 * The lines under a form field, each in its own colour (project rule, 6 Nov 2026):
 * hint = grey (information), warning = yellow (look before you continue), error = red (must be fixed).
 * Yellow text itself would be unreadable on white (#f5a623 is 2:1), so a warning is amber text (#a15c07, 5.2:1)
 * behind a yellow ⚠ sign; an error is red text (#b42318, 6.6:1) behind a red ✕ sign.
 */
export function FieldMessages({ id, hint, warning, error }) {
  return (
    <>
      {hint && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-muted">
          {hint}
        </p>
      )}
      {warning && (
        <p
          id={`${id}-warning`}
          data-tone="warning"
          className="mt-1 flex items-start gap-1 text-xs font-medium text-warning"
        >
          <span aria-hidden="true" className="text-accent-500">
            ⚠
          </span>
          <span>{warning}</span>
        </p>
      )}
      {error && (
        <p id={`${id}-error`} data-tone="error" className="mt-1 flex items-start gap-1 text-sm text-danger">
          <span aria-hidden="true">✕</span>
          <span>{error}</span>
        </p>
      )}
    </>
  )
}
