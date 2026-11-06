import { Badge } from '../../components/ui/Badge'

export function EnrolmentStatusBadge({ status }) {
  return status === 'ENROLLED' ? <Badge tone="success">Enrolled</Badge> : <Badge>Cancelled</Badge>
}
