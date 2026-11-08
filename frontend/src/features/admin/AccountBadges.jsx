import { Badge } from '../../components/ui/Badge'

/** Enabled or disabled, and whether the email address is verified. */
export function AccountBadges({ user }) {
  return (
    <span className="inline-flex flex-wrap gap-1">
      {user.enabled ? <Badge tone="success">Enabled</Badge> : <Badge tone="danger">Disabled</Badge>}
      {user.emailVerified ? <Badge>Email verified</Badge> : <Badge tone="warning">Email not verified</Badge>}
    </span>
  )
}

/**
 * The farmer record of a farmer account. The administrator cannot open farmer records (G6, decision A2),
 * so only its number is shown. A farmer account without one cannot use the system until it is linked.
 */
export function FarmerLink({ user }) {
  if (user.role !== 'FARMER') return '—'
  return user.farmerId != null ? `Farmer #${user.farmerId}` : <Badge tone="warning">Not linked</Badge>
}
