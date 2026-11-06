import { Badge } from '../../components/ui/Badge'

/** Whether farmers may enrol in the season right now (SeasonResponse.enrolmentOpen). */
export function WindowBadge({ open }) {
  return open ? <Badge tone="success">Enrolment open</Badge> : <Badge>Enrolment closed</Badge>
}
