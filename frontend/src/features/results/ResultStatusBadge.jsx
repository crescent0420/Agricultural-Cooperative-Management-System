import { Badge } from '../../components/ui/Badge'

const LABELS = { DRAFT: 'Draft', SUBMITTED: 'Submitted', PUBLISHED: 'Published' }

/** Draft (grey), Submitted: waiting for the Agronomist (amber), Published (green). */
export function ResultStatusBadge({ status }) {
  const tone = status === 'PUBLISHED' ? 'success' : status === 'SUBMITTED' ? 'warning' : 'neutral'
  return <Badge tone={tone}>{LABELS[status] ?? status}</Badge>
}
