import { Link } from 'react-router'
import { PlacesCell } from './OfferingsTable'
import { useOffering } from './useOffering'

/** Offering page, Overview tab (URD FR-OFF-05). */
export function OfferingOverviewTab() {
  const o = useOffering()
  const rows = [
    { label: 'Crop programme', value: o.programmeName },
    {
      label: 'Season',
      value: (
        <Link to={`/seasons/${o.seasonId}`} className="font-medium text-brand-700 hover:underline">
          {o.seasonName}
        </Link>
      ),
    },
    { label: 'District', value: o.districtName },
    { label: 'Field Officer', value: o.fieldOfficerUsername },
    { label: 'Hectares per participant', value: `${o.hectaresPerParticipant} ha` },
    { label: 'Enrolled / maximum', value: <PlacesCell offering={o} /> },
    { label: 'Places left', value: o.remainingPlaces },
  ]
  return (
    <section className="rounded-lg border border-line bg-white p-6">
      <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
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
