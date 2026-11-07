import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { EligibilityChecklist } from './EligibilityChecklist'

/** One offering for the farmer: details, the eligibility checklist (items), and Enrol (or Enrolled). */
export function OfferingCard({ offering, items, met, enrolled, onEnrol }) {
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
        <span className="flex shrink-0 items-center gap-1.5">
          {!enrolled && (
            <Badge tone={met === items.length ? 'success' : 'neutral'}>
              {met} of {items.length} met
            </Badge>
          )}
          {offering.remainingPlaces === 0 && <Badge tone="danger">Full</Badge>}
        </span>
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
