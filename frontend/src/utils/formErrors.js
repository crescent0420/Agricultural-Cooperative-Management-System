/**
 * Puts the backend's 400 field errors under the matching form fields.
 * The backend names list items "bands[0].grade"; React Hook Form calls them "bands.0.grade".
 * Returns true when at least one field received a message.
 */
export function applyFieldErrors(error, setError, knownFields) {
  let applied = false
  for (const issue of error.fieldErrors) {
    const field = issue.field.replace(/\[(\d+)\]/g, '.$1')
    const root = field.split('.')[0]
    if (knownFields.includes(root)) {
      setError(field, { message: issue.message })
      applied = true
    }
  }
  return applied
}
