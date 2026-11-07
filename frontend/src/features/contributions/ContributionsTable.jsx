import { Link } from 'react-router'
import { DataTable } from '../../components/ui/DataTable'
import { formatAmount } from '../../utils/format'
import { ContributionFlags } from './ContributionFlags'

/** Contributions with every figure exactly as the backend sends it (FR-FIN-01: nothing calculated here). */
export function ContributionsTable({ query, sort, onSort, emptyMessage, showFarmer = true }) {
  const columns = [
    showFarmer && {
      header: 'Farmer',
      cell: (c) => (
        <Link to={`/contributions/${c.id}`} className="font-medium text-brand-700 hover:underline">
          {c.farmerName}
          <span className="block font-mono text-xs font-normal text-muted">{c.membershipNumber}</span>
        </Link>
      ),
    },
    {
      header: 'Season',
      cell: (c) =>
        showFarmer ? (
          c.seasonName
        ) : (
          <Link to={`/contributions/${c.id}`} className="font-medium text-brand-700 hover:underline">
            {c.seasonName}
          </Link>
        ),
    },
    { header: 'Due', sortField: 'amountDue', className: 'text-right', cell: (c) => formatAmount(c.amountDue) },
    { header: 'Paid (confirmed)', className: 'text-right', cell: (c) => formatAmount(c.confirmedPaid) },
    { header: 'Pending', className: 'text-right', cell: (c) => formatAmount(c.pendingAmount) },
    {
      header: 'Outstanding',
      className: 'text-right',
      cell: (c) => <span className="font-semibold">{formatAmount(c.outstanding)}</span>,
    },
    { header: 'Paid', className: 'text-right', cell: (c) => `${c.paidPercent}%` },
    { header: 'Allows', cell: (c) => <ContributionFlags contribution={c} /> },
  ].filter(Boolean)
  return (
    <DataTable
      caption="Contributions"
      columns={columns}
      rows={query.data?.content}
      rowKey={(c) => c.id}
      loading={query.isPending}
      error={query.error}
      emptyMessage={emptyMessage}
      sort={sort}
      onSort={onSort}
    />
  )
}
