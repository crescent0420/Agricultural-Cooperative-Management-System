import { describe, expect, it } from 'vitest'
import { uwase } from '../../test/farmers'
import { contribution2026B } from '../../test/enrolments'
import { fullOffering, maizeOffering, season2026B } from '../../test/seasons'
import { eligibilityChecklist, rankOfferings } from './eligibility'

const ready = {
  offering: maizeOffering,
  season: season2026B,
  profile: uwase,
  contribution: contribution2026B,
  missing: [],
}
const byLabel = (items) => Object.fromEntries(items.map((i) => [i.label, i]))

describe('eligibilityChecklist (FR-ENR-01)', () => {
  it('ticks every line when the backend data says so', () => {
    const items = eligibilityChecklist(ready)
    expect(items.map((i) => i.ok)).toEqual([true, true, true, true, true])
    expect(byLabel(items)['At least 30% of the contribution paid'].reason).toBe(
      'You have paid 40% of the 2026B contribution.',
    )
    expect(byLabel(items)['Places left'].reason).toBe('28 of 40 places left.')
  })

  it('gives the reason for each cross', () => {
    const items = byLabel(
      eligibilityChecklist({
        offering: fullOffering,
        season: { ...season2026B, enrolmentOpen: false },
        profile: { ...uwase, status: 'SUSPENDED' },
        contribution: { ...contribution2026B, paidPercent: 10, enrolmentAllowed: false },
        missing: [{ id: 4, name: 'Pest and disease control' }],
      }),
    )
    expect(items['Enrolment window open']).toMatchObject({ ok: false, reason: 'Enrolment for 2026B is closed.' })
    expect(items['Your status is Active']).toMatchObject({ ok: false, reason: 'Your status is suspended.' })
    expect(items['At least 30% of the contribution paid'].ok).toBe(false)
    expect(items['Prerequisite trainings completed'].reason).toBe('Still needed: Pest and disease control.')
    expect(items['Places left']).toMatchObject({ ok: false, reason: 'This offering is full.' })
  })

  it('uses the backend’s enrolmentAllowed flag, not its own sum', () => {
    const items = byLabel(
      eligibilityChecklist({ ...ready, contribution: { ...contribution2026B, enrolmentAllowed: false } }),
    )
    expect(items['At least 30% of the contribution paid'].ok).toBe(false)
  })

  it('says when no contribution is recorded, and marks unknown lines while loading', () => {
    const none = byLabel(eligibilityChecklist({ ...ready, contribution: null }))
    expect(none['At least 30% of the contribution paid']).toMatchObject({
      ok: false,
      reason: 'No input contribution is recorded for you in 2026B.',
    })
    const loading = eligibilityChecklist({ offering: maizeOffering })
    expect(loading.slice(0, 4).every((i) => i.ok === null)).toBe(true)
  })
})

describe('rankOfferings (Offerings for me order, 7 Nov 2026)', () => {
  const row = (id, oks, enrolled = false) => ({
    offering: { id },
    enrolled,
    items: oks.map((ok) => ({ ok })),
  })

  it('puts offerings meeting every requirement first, then by how many are met, enrolled ones last', () => {
    const ranked = rankOfferings([
      row(1, [true, false, false, true, true]), // 3 met
      row(2, [true, true, true, true, true], true), // enrolled
      row(3, [true, true, true, true, true]), // all met
      row(4, [true, true, true, true, false]), // 4 met
      row(5, [true, null, false, true, true]), // 3 met (unknown is not met), after 1: same count, later in the list
    ])
    expect(ranked.map((r) => r.offering.id)).toEqual([3, 4, 1, 5, 2])
    expect(ranked.map((r) => r.met)).toEqual([5, 4, 3, 3, 5])
  })
})
