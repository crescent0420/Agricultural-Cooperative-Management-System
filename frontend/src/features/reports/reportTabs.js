/** The four management reports, each shown only to holders of its permission (URD FR-REP-01..04). */
export const REPORT_TABS = [
  { path: 'farmers', label: 'Farmers per cooperative', permission: 'REPORT_FARMERS_PER_COOPERATIVE', bySeason: false },
  { path: 'capacity', label: 'Offering capacity', permission: 'REPORT_OFFERING_CAPACITY', bySeason: true },
  {
    path: 'contributions',
    label: 'Outstanding contributions',
    permission: 'REPORT_OUTSTANDING_CONTRIBUTIONS',
    bySeason: true,
  },
  { path: 'grades', label: 'Grade distribution', permission: 'REPORT_GRADE_DISTRIBUTION', bySeason: true },
]

export const REPORT_PERMISSIONS = REPORT_TABS.map((t) => t.permission)
