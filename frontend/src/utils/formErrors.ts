import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiError } from '../api/errors'

/**
 * Puts the backend's 400 field errors under the matching form fields.
 * The backend names list items "bands[0].grade"; React Hook Form calls them "bands.0.grade".
 * Returns true when at least one field received a message.
 */
export function applyFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  knownFields: readonly string[],
): boolean {
  let applied = false
  for (const issue of error.fieldErrors) {
    const field = issue.field.replace(/\[(\d+)\]/g, '.$1')
    const root = field.split('.')[0]
    if (knownFields.includes(root)) {
      setError(field as Path<T>, { message: issue.message })
      applied = true
    }
  }
  return applied
}
