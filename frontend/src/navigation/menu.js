/**
 * @typedef {object} MenuItem
 * @property {string} label
 * @property {string} to
 * @property {import('./access').Requirement} requires
 * @property {string} arrivesIn  The feature branch that replaces this page's placeholder.
 */

/**
 * @typedef {object} MenuGroup
 * @property {string | null} label  null = no heading (the first group)
 * @property {MenuItem[]} items
 */

/** @type {MenuGroup[]} The sidebar, as in URD section 13. Each item opens only for users holding its permissions. */
export const MENU = [
  {
    label: null,
    items: [{ label: 'Dashboard', to: '/', requires: {}, arrivesIn: 'dashboards' }],
  },
  {
    label: 'My area',
    items: [
      { label: 'My profile', to: '/my/profile', requires: { allOf: ['PROFILE_READ_OWN'] }, arrivesIn: 'farmers' },
      {
        label: 'Offerings for me',
        to: '/my/offerings',
        requires: { allOf: ['ENROLMENT_CREATE'] },
        arrivesIn: 'enrolment',
      },
      {
        label: 'My enrolments',
        to: '/my/enrolments',
        requires: { allOf: ['ENROLMENT_READ_OWN'] },
        arrivesIn: 'enrolment',
      },
      {
        label: 'My contributions',
        to: '/my/contributions',
        requires: { allOf: ['CONTRIBUTION_READ_OWN'] },
        arrivesIn: 'payments',
      },
      {
        label: 'My results',
        to: '/my/results',
        requires: { allOf: ['RESULT_READ_OWN'] },
        arrivesIn: 'inspections and results',
      },
      { label: 'My permits', to: '/my/permits', requires: { allOf: ['PERMIT_READ_OWN'] }, arrivesIn: 'permits' },
    ],
  },
  {
    label: 'Operations',
    items: [
      { label: 'Farmers', to: '/farmers', requires: { allOf: ['FARMER_LIST'] }, arrivesIn: 'farmers' },
      {
        label: 'Offerings',
        to: '/offerings',
        // Farmers also hold OFFERING_READ but use "Offerings for me"; only staff can list an offering's farmers.
        requires: { allOf: ['OFFERING_READ', 'ENROLMENT_LIST_FOR_OFFERING'] },
        arrivesIn: 'seasons and offerings',
      },
      {
        label: 'My assigned offerings',
        to: '/offerings/mine',
        requires: { allOf: ['OFFERING_READ_ASSIGNED'] },
        arrivesIn: 'seasons and offerings',
      },
      { label: 'Seasons', to: '/seasons', requires: { allOf: ['SEASON_READ'] }, arrivesIn: 'seasons and offerings' },
    ],
  },
  {
    label: 'Finance',
    items: [
      {
        label: 'Contributions',
        to: '/contributions',
        requires: { allOf: ['CONTRIBUTION_LIST'] },
        arrivesIn: 'payments',
      },
    ],
  },
  {
    label: 'Permits',
    items: [
      { label: 'Verify a permit', to: '/permits/verify', requires: { allOf: ['PERMIT_VERIFY'] }, arrivesIn: 'permits' },
      {
        label: 'Permit attempts',
        to: '/permits/attempts',
        requires: { allOf: ['PERMIT_ATTEMPT_READ'] },
        arrivesIn: 'permits',
      },
    ],
  },
  {
    label: 'Insight',
    items: [
      {
        label: 'Reports',
        to: '/reports',
        requires: {
          anyOf: [
            'REPORT_FARMERS_PER_COOPERATIVE',
            'REPORT_OFFERING_CAPACITY',
            'REPORT_OUTSTANDING_CONTRIBUTIONS',
            'REPORT_GRADE_DISTRIBUTION',
          ],
        },
        arrivesIn: 'reports',
      },
    ],
  },
  {
    label: 'Setup',
    items: [
      {
        label: 'Reference data',
        to: '/reference',
        requires: { allOf: ['REFERENCE_READ'] },
        arrivesIn: 'reference data',
      },
      {
        label: 'Grading scale',
        to: '/grading-scale',
        requires: { allOf: ['GRADING_SCALE_READ'] },
        arrivesIn: 'reference data',
      },
    ],
  },
  {
    label: 'Administration',
    items: [
      { label: 'Users', to: '/admin/users', requires: { allOf: ['USER_MANAGE'] }, arrivesIn: 'administration' },
      {
        label: 'Audit log',
        to: '/admin/audit-log',
        requires: { allOf: ['AUDIT_LOG_READ'] },
        arrivesIn: 'administration',
      },
    ],
  },
  {
    label: 'Account',
    items: [
      {
        label: 'Permission matrix',
        to: '/permissions',
        requires: { allOf: ['PERMISSION_MATRIX_READ'] },
        arrivesIn: 'administration',
      },
    ],
  },
]
