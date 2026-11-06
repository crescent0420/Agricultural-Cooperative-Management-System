import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { getMissingPrerequisites } from '../../api/farmers'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { EligibilityChecklist } from './EligibilityChecklist'
import { eligibilityChecklist } from './eligibility'

/** One offering for the farmer: details, the eligibility checklist, and Enrol (or Enrolled). */
export function OfferingCard({ offering, season, profile, contribution, enrolled, onEnrol }) {
  // One request per programme; offerings of the same programme share the answer.
  const missing = useQuery({
    queryKey: ['farmers', 'missing', profile?.id, String(offering.programmeId)],
    queryFn: () => getMissingPrerequisites(profile.id, offering.programmeId),
    enabled: profile != null,
  })
  const items = eligibilityChecklist({ offering, season, profile, contribution, missing: missing.data })

  return (
    <article
      aria-label={`${offering.programmeName}, ${offering.seasonName}`}
      className="flex flex-col rounded-lg border border-line bg-white p-5"
    >
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">{offering.programmeName}</h2>
          <p className="text-sm text-muted">
            {offering.seasonName} · {offering.hectaresPerParticipant} ha · Field Officer {offering.fieldOfficerUsername}
          </p>
        </div>
        {offering.remainingPlaces === 0 && <Badge tone="danger">Full</Badge>}
      </div>
      <EligibilityChecklist items={items} />
      <div className="mt-4 flex items-center gap-3 border-t border-line pt-4">
        {enrolled ? (
          <>
            <Badge tone="success">Enrolled</Badge>
            <Link to="/my/enrolments" className="text-sm font-semibold text-brand-700 hover:underline">
              See my enrolments
            </Link>
          </>
        ) : (
          <>
            <Button onClick={() => onEnrol(offering)}>Enrol</Button>
            <span className="text-xs text-muted">The checklist is a guide; the system decides when you enrol.</span>
          </>
        )}
      </div>
    </article>
  )
}
