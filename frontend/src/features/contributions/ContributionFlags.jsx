import { Badge } from '../../components/ui/Badge'

/**
 * The backend's two payment rules, shown as they come (URD FR-FIN-01, FR-FIN-07):
 * enrolmentAllowed (at least 30% paid) and inputCollectionAllowed (100% paid).
 */
export function ContributionFlags({ contribution }) {
  return (
    <span className="inline-flex flex-wrap gap-1.5">
      {contribution.enrolmentAllowed ? <Badge tone="success">May enrol</Badge> : <Badge>Cannot enrol yet</Badge>}
      {contribution.inputCollectionAllowed ? (
        <Badge tone="success">May collect inputs</Badge>
      ) : (
        <Badge>Cannot collect yet</Badge>
      )}
    </span>
  )
}
