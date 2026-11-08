import { apiGet } from './client'

/** FR-ADM-06 (AUDIT_LOG_READ): sortBy id, occurredAt, eventType; newest first by default. No filters (G10). */
export function listAuditLogs(params) {
  return apiGet('/audit-logs', { ...params })
}
