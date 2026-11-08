import { apiGet } from './client'

/**
 * FR-ADM-07 (PERMISSION_MATRIX_READ, every role): { rows: [{ permission, group, description, roles }],
 * byRole: { ROLE: [permission, …] } }, both in the backend's own order.
 */
export function getPermissionMatrix() {
  return apiGet('/permissions')
}
