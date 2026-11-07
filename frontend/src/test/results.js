/** Test data shaped like the backend's inspection and result answers (offering 12: Maize, 2026B, Huye). */
import { uwase } from './farmers'

export const soil = { id: 101, offeringId: 12, name: 'Soil and land preparation', maxScore: 20, weight: 40 }
export const pests = { id: 102, offeringId: 12, name: 'Pest and disease control', maxScore: 10, weight: 60 }

/** @type {import('../types/result').InspectionList} */
export const completeList = {
  offeringId: 12,
  inspections: [soil, pests],
  totalWeight: 100,
  weightsComplete: true,
  frozen: false,
}

export const line = (inspection, score, extra = {}) => ({
  inspectionId: inspection.id,
  inspectionName: inspection.name,
  maxScore: inspection.maxScore,
  weight: inspection.weight,
  originalScore: score,
  currentScore: score,
  corrected: false,
  ...extra,
})

/** @type {import('../types/result').Result} */
export const draftResult = {
  id: 80,
  enrolmentId: 50,
  offeringId: 12,
  programmeName: 'Maize',
  seasonName: '2026B',
  farmerId: uwase.id,
  membershipNumber: uwase.membershipNumber,
  farmerName: uwase.fullName,
  status: 'DRAFT',
  scores: [line(soil, 15), line(pests, null)],
  finalScore: null,
  grade: null,
  submittedAt: null,
  publishedAt: null,
  correctionCount: 0,
}

export const submittedResult = {
  ...draftResult,
  status: 'SUBMITTED',
  scores: [line(soil, 15), line(pests, 8)],
  finalScore: 78,
  grade: 'B',
  submittedAt: '2026-11-01T09:00:00Z',
}

export const publishedResult = { ...submittedResult, status: 'PUBLISHED', publishedAt: '2026-11-02T09:00:00Z' }

export const summaryOf = (result) => ({
  id: result.id,
  enrolmentId: result.enrolmentId,
  offeringId: result.offeringId,
  programmeName: result.programmeName,
  seasonName: result.seasonName,
  farmerId: result.farmerId,
  membershipNumber: result.membershipNumber,
  farmerName: result.farmerName,
  status: result.status,
  finalScore: result.finalScore,
  grade: result.grade,
  publishedAt: result.publishedAt,
})

export const correction = {
  id: 5,
  inspectionId: pests.id,
  inspectionName: pests.name,
  originalScore: 8,
  correctedScore: 9,
  originalFinalScore: 78,
  correctedFinalScore: 84,
  originalGrade: 'B',
  correctedGrade: 'A',
  reason: 'Re-inspection after the appeal',
  correctedById: 6,
  correctedByUsername: 'demo.agronomist',
  correctedAt: '2026-11-03T09:00:00Z',
}
