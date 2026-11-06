import { StatusBadge } from './StatusBadge'

/** A farmer's record as a list of labelled values; used on the farmer page and on "My profile". */
export function FarmerDetailsCard({ farmer, actions }) {
  const rows = [
    { label: 'Membership number', value: <span className="font-mono">{farmer.membershipNumber}</span> },
    { label: 'Full name', value: farmer.fullName },
    { label: 'National ID', value: <span className="font-mono">{farmer.nationalId}</span> },
    { label: 'Phone number', value: farmer.phoneNumber || '—' },
    { label: 'Cooperative', value: farmer.cooperativeName },
    { label: 'District', value: farmer.districtName },
    { label: 'Status', value: <StatusBadge status={farmer.status} /> },
  ]
  return (
    <section className="rounded-lg border border-line bg-white p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Farmer details</h2>
        {actions}
      </div>
      <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
        {rows.map(({ label, value }) => (
          <div key={label}>
            <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</dt>
            <dd className="mt-1 text-ink">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}
