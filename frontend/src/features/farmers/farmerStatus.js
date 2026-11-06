// Farmer statuses as the screens show them.

/** @type {import('../../types/farmer').FarmerStatus[]} */
export const FARMER_STATUSES = ['ACTIVE', 'SUSPENDED', 'WITHDRAWN', 'RETIRED']

export const STATUS_LABELS = {
  ACTIVE: 'Active',
  SUSPENDED: 'Suspended',
  WITHDRAWN: 'Withdrawn',
  RETIRED: 'Retired',
}

/** What each status means for the farmer, taken from the course brief's rules (enrolment and permits need Active). */
export const STATUS_MEANINGS = {
  ACTIVE: 'May enrol in programmes and receive collection permits when the other conditions are met.',
  SUSPENDED: 'May not enrol or receive a collection permit while suspended.',
  WITHDRAWN: 'Has left the Union; may not enrol or receive a collection permit. Can be reactivated.',
  RETIRED: 'Final: the status can no longer be changed.',
}

/**
 * The changes the backend accepts, copied from com.acms.enums.FarmerStatus.canChangeTo.
 * URD FR-FAR-05 asks the dropdown to offer only these; the backend still checks every change (422 otherwise).
 * farmerStatus.test.js fails if this table and the backend's rule ever disagree.
 */
export const NEXT_STATUSES = {
  ACTIVE: ['SUSPENDED', 'WITHDRAWN', 'RETIRED'],
  SUSPENDED: ['ACTIVE', 'WITHDRAWN', 'RETIRED'],
  WITHDRAWN: ['ACTIVE'],
  RETIRED: [],
}
