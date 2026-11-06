/** Test data shaped like the backend's season, window and offering answers. */

/** @type {import('../types/season').Season} */
export const season2027A = {
  id: 4,
  name: '2027A',
  startsOn: '2027-02-01',
  endsOn: '2027-07-31',
  maxLandHectares: 2.5,
  distributionDate: '2027-02-15',
  enrolmentOpen: false,
  openWindowId: null,
  createdAt: '2026-11-01T08:00:00Z',
  updatedAt: '2026-11-01T08:00:00Z',
}

export const season2026B = {
  ...season2027A,
  id: 3,
  name: '2026B',
  startsOn: '2026-09-01',
  endsOn: '2027-01-31',
  maxLandHectares: 2,
  distributionDate: '2026-09-15',
  enrolmentOpen: true,
  openWindowId: 9,
}

export const openWindowRow = {
  id: 9,
  seasonId: 3,
  seasonName: '2026B',
  status: 'OPEN',
  openedAt: '2026-09-01T07:00:00Z',
  openedBy: 'demo.registrar',
  closedAt: null,
  closedBy: null,
}

/** @type {import('../types/season').Offering} */
export const maizeOffering = {
  id: 12,
  programmeId: 30,
  programmeName: 'Maize',
  districtId: 15,
  districtName: 'Huye',
  seasonId: 3,
  seasonName: '2026B',
  fieldOfficerId: 5,
  fieldOfficerUsername: 'demo.officer',
  hectaresPerParticipant: 0.5,
  maxParticipants: 40,
  enrolledCount: 12,
  remainingPlaces: 28,
  createdAt: '2026-09-02T08:00:00Z',
  updatedAt: '2026-09-02T08:00:00Z',
}

export const fullOffering = {
  ...maizeOffering,
  id: 13,
  programmeName: 'Beans',
  programmeId: 31,
  enrolledCount: 40,
  remainingPlaces: 0,
}

export const maize = { id: 30, name: 'Maize', districtId: 15, districtName: 'Huye', prerequisites: [] }
export const potatoes = { id: 40, name: 'Irish potatoes', districtId: 21, districtName: 'Musanze', prerequisites: [] }
export const huyeOfficer = { id: 5, username: 'demo.officer', districtId: 15, districtName: 'Huye' }
export const huyeOfficer2 = { id: 8, username: 'huye.officer2', districtId: 15, districtName: 'Huye' }
export const musanzeOfficer = { id: 6, username: 'demo.officer.musanze', districtId: 21, districtName: 'Musanze' }
