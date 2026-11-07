import { Badge } from '../../components/ui/Badge'

const LOOK = {
  PENDING: { tone: 'warning', label: 'Pending' },
  CONFIRMED: { tone: 'success', label: 'Confirmed' },
  FAILED: { tone: 'danger', label: 'Failed' },
}

export function PaymentStatusBadge({ status }) {
  const look = LOOK[status] ?? { tone: 'neutral', label: status }
  return <Badge tone={look.tone}>{look.label}</Badge>
}
