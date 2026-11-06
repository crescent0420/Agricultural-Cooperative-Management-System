import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { DataTable } from '../../components/ui/DataTable'

/** "12 / 40" with a Full badge when no place is left (URD FR-OFF-01). The numbers come from the backend. */
export function PlacesCell({ offering }) {
  return (
    <span className="inline-flex items-center gap-2">
      {offering.enrolledCount} / {offering.maxParticipants}
      {offering.remainingPlaces === 0 && <Badge tone="danger">Full</Badge>}
    </span>
  )
}

/** The offerings table shared by "Offerings" and "My assigned offerings". */
export function OfferingsTable({ query, sort, onSort, emptyMessage }) {
  return (
    <DataTable
      caption="Programme offerings"
      columns={[
        {
          header: 'Programme',
          cell: (o) => (
            <Link to={`/offerings/${o.id}`} className="font-medium text-brand-700 hover:underline">
              {o.programmeName}
            </Link>
          ),
        },
        { header: 'Season', cell: (o) => o.seasonName },
        { header: 'District', cell: (o) => o.districtName },
        { header: 'Field Officer', cell: (o) => o.fieldOfficerUsername },
        {
          header: 'Ha per farmer',
          sortField: 'hectaresPerParticipant',
          className: 'text-right',
          cell: (o) => o.hectaresPerParticipant,
        },
        { header: 'Enrolled / max', sortField: 'enrolledCount', cell: (o) => <PlacesCell offering={o} /> },
        { header: 'Places left', className: 'text-right', cell: (o) => o.remainingPlaces },
      ]}
      rows={query.data?.content}
      rowKey={(o) => o.id}
      loading={query.isPending}
      error={query.error}
      emptyMessage={emptyMessage}
      sort={sort}
      onSort={onSort}
    />
  )
}
