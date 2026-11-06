/** The ids of the messages shown, for aria-describedby, so screen readers read them with the field. */
export function describedBy(id, { hint, warning, error }) {
  return (
    [hint && `${id}-hint`, warning && `${id}-warning`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  )
}
