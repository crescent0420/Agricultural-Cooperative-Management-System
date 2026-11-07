import { apiGet } from './client'

// Management reports (URD FR-REP-01..04). Each answer is a whole report, not a page:
// { report, basis, scope: { districtId, seasonId, fieldOfficerId }, generatedAt, rows, totals }.
// Every filter is optional. District staff get their own district whatever they send (another one → 403);
// a Field Officer also sees only the offerings assigned to them (capacity and grades). Unknown district or season → 404.

const clean = (filters) => Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== '' && v != null))

/** FR-REP-01 (REPORT_FARMERS_PER_COOPERATIVE). No season: farmers are counted by their current status. */
export function getFarmersPerCooperative({ districtId } = {}) {
  return apiGet('/reports/farmers-per-cooperative', clean({ districtId }))
}

/** FR-REP-02 (REPORT_OFFERING_CAPACITY). */
export function getOfferingCapacity({ seasonId, districtId } = {}) {
  return apiGet('/reports/offering-capacity', clean({ seasonId, districtId }))
}

/** FR-REP-03 (REPORT_OUTSTANDING_CONTRIBUTIONS). */
export function getOutstandingContributions({ seasonId, districtId } = {}) {
  return apiGet('/reports/outstanding-contributions', clean({ seasonId, districtId }))
}

/** FR-REP-04 (REPORT_GRADE_DISTRIBUTION). Only published results count. */
export function getGradeDistribution({ seasonId, districtId } = {}) {
  return apiGet('/reports/grade-distribution', clean({ seasonId, districtId }))
}
