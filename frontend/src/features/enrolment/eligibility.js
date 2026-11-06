/**
 * The eligibility checklist of URD FR-ENR-01: one line per condition, each with a tick or a cross and a reason.
 * Every answer is read from the backend's data (enrolmentOpen, the farmer's status, the contribution's own
 * enrolmentAllowed flag, the missing-prerequisites list, remainingPlaces); nothing is worked out here.
 * Advice only: the backend makes the decision when the farmer clicks Enrol.
 *
 * @param {object} data
 * @param {import('../../types/season').Offering} data.offering
 * @param {import('../../types/season').Season | undefined} data.season
 * @param {import('../../types/farmer').Farmer | undefined} data.profile
 * @param {import('../../types/enrolment').Contribution | null | undefined} data.contribution  null = none recorded
 * @param {import('../../types/reference').Training[] | undefined} data.missing  undefined = still loading
 * @returns {{ label: string, ok: boolean | null, reason: string }[]}  ok null = not known yet
 */
export function eligibilityChecklist({ offering, season, profile, contribution, missing }) {
  const unknown = (label) => ({ label, ok: null, reason: 'Checking…' })
  return [
    season
      ? {
          label: 'Enrolment window open',
          ok: season.enrolmentOpen,
          reason: season.enrolmentOpen
            ? `Enrolment for ${season.name} is open.`
            : `Enrolment for ${season.name} is closed.`,
        }
      : unknown('Enrolment window open'),
    profile
      ? {
          label: 'Your status is Active',
          ok: profile.status === 'ACTIVE',
          reason: profile.status === 'ACTIVE' ? 'You are Active.' : `Your status is ${profile.status.toLowerCase()}.`,
        }
      : unknown('Your status is Active'),
    contribution === undefined
      ? unknown('At least 30% of the contribution paid')
      : contribution === null
        ? {
            label: 'At least 30% of the contribution paid',
            ok: false,
            reason: `No input contribution is recorded for you in ${offering.seasonName}.`,
          }
        : {
            label: 'At least 30% of the contribution paid',
            ok: contribution.enrolmentAllowed,
            reason: `You have paid ${contribution.paidPercent}% of the ${offering.seasonName} contribution.`,
          },
    missing === undefined
      ? unknown('Prerequisite trainings completed')
      : {
          label: 'Prerequisite trainings completed',
          ok: missing.length === 0,
          reason:
            missing.length === 0
              ? 'All prerequisite trainings completed.'
              : `Still needed: ${missing.map((t) => t.name).join(', ')}.`,
        },
    {
      label: 'Places left',
      ok: offering.remainingPlaces > 0,
      reason:
        offering.remainingPlaces > 0
          ? `${offering.remainingPlaces} of ${offering.maxParticipants} places left.`
          : 'This offering is full.',
    },
  ]
}
