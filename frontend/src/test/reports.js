/** Test data shaped like the backend's ReportResponse answers. */

const meta = (report, scope = {}) => ({
  report,
  basis: `${report}: how it is calculated.`,
  scope: { districtId: null, seasonId: null, fieldOfficerId: null, ...scope },
  generatedAt: '2026-11-07T08:15:00Z',
})

export const farmersReport = {
  ...meta('Farmers per cooperative'),
  rows: [
    {
      cooperativeId: 1,
      cooperativeName: 'Huye Maize Growers Cooperative',
      districtId: 15,
      districtName: 'Huye',
      active: 120,
      suspended: 4,
      withdrawn: 2,
      retired: 1,
      total: 127,
    },
    {
      cooperativeId: 2,
      cooperativeName: 'Musanze Potato Cooperative',
      districtId: 21,
      districtName: 'Musanze',
      active: 0,
      suspended: 0,
      withdrawn: 0,
      retired: 0,
      total: 0,
    },
  ],
  totals: { cooperatives: 2, active: 120, suspended: 4, withdrawn: 2, retired: 1, total: 127 },
}

export const capacityReport = {
  ...meta('Enrolments and capacity per programme offering', { seasonId: 3 }),
  rows: [
    {
      offeringId: 9,
      programmeId: 30,
      programmeName: 'Maize',
      seasonId: 3,
      seasonName: '2026B',
      districtId: 15,
      districtName: 'Huye',
      fieldOfficerId: 5,
      fieldOfficerUsername: 'demo.officer',
      maxParticipants: 40,
      enrolled: 40,
      cancelled: 3,
      placesLeft: 0,
      capacityUsedPercent: 100,
      full: true,
      hectaresPerParticipant: 1.5,
      hectaresEnrolled: 52.5,
    },
    {
      offeringId: 10,
      programmeId: 31,
      programmeName: 'Beans',
      seasonId: 3,
      seasonName: '2026B',
      districtId: 15,
      districtName: 'Huye',
      fieldOfficerId: null,
      fieldOfficerUsername: null,
      maxParticipants: 30,
      enrolled: 12,
      cancelled: 0,
      placesLeft: 18,
      capacityUsedPercent: 40,
      full: false,
      hectaresPerParticipant: 1,
      hectaresEnrolled: 12,
    },
  ],
  totals: {
    offerings: 2,
    fullOfferings: 1,
    maxParticipants: 70,
    enrolled: 52,
    cancelled: 3,
    placesLeft: 18,
    capacityUsedPercent: 74.29,
    hectaresEnrolled: 64.5,
  },
}

export const contributionsReport = {
  ...meta('Outstanding contributions per season'),
  rows: [
    {
      seasonId: 3,
      seasonName: '2026B',
      districtId: 15,
      districtName: 'Huye',
      contributions: 52,
      amountDue: 2600000,
      confirmedPaid: 1950000.5,
      pendingPayments: 100000,
      outstanding: 649999.5,
      collectedPercent: 75,
      fullyPaid: 30,
      partlyPaid: 15,
      unpaid: 7,
    },
  ],
  totals: {
    contributions: 52,
    amountDue: 2600000,
    confirmedPaid: 1950000.5,
    pendingPayments: 100000,
    outstanding: 649999.5,
    collectedPercent: 75,
    fullyPaid: 30,
    partlyPaid: 15,
    unpaid: 7,
  },
}

const grades = (a, b, c) => [
  { grade: 'A', count: a, percent: a + b + c ? Math.round((a * 10000) / (a + b + c)) / 100 : null },
  { grade: 'B', count: b, percent: a + b + c ? Math.round((b * 10000) / (a + b + c)) / 100 : null },
  { grade: 'C', count: c, percent: a + b + c ? Math.round((c * 10000) / (a + b + c)) / 100 : null },
]

export const gradesReport = {
  ...meta('Grade distribution per programme offering'),
  rows: [
    {
      offeringId: 9,
      programmeName: 'Maize',
      seasonId: 3,
      seasonName: '2026B',
      districtId: 15,
      districtName: 'Huye',
      fieldOfficerUsername: 'demo.officer',
      activeEnrolments: 40,
      published: 3,
      notYetPublished: 37,
      totalFinalScore: 225,
      averageFinalScore: 75,
      grades: grades(1, 2, 0),
    },
  ],
  totals: { offerings: 1, published: 3, notYetPublished: 37, averageFinalScore: 75, grades: grades(1, 2, 0) },
}
