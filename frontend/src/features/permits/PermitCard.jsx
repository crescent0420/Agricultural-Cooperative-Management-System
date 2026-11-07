import { formatDate, formatDateTime } from '../../utils/format'

/** A permit as the farmer shows it at the store: the number in large print, then what it is for. */
export function PermitCard({ permit }) {
  return (
    <article
      aria-label={`Permit ${permit.permitNumber}`}
      className="rounded-lg border-2 border-brand-600 bg-white p-5 break-inside-avoid"
    >
      <p className="text-xs font-semibold tracking-wide text-muted uppercase">Collection permit</p>
      <p className="my-2 font-mono text-2xl font-bold tracking-wider text-ink sm:text-3xl">{permit.permitNumber}</p>
      <dl className="grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
        <div>
          <dt className="text-muted">Farmer</dt>
          <dd className="font-medium">
            {permit.farmerName} <span className="font-mono text-xs">{permit.membershipNumber}</span>
          </dd>
        </div>
        <div>
          <dt className="text-muted">Programme</dt>
          <dd className="font-medium">
            {permit.programmeName}, {permit.seasonName}
          </dd>
        </div>
        <div>
          <dt className="text-muted">Distribution day</dt>
          <dd className="font-medium">{formatDate(permit.distributionDate)}</dd>
        </div>
        <div>
          <dt className="text-muted">Issued</dt>
          <dd>{formatDateTime(permit.issuedAt)}</dd>
        </div>
      </dl>
    </article>
  )
}
