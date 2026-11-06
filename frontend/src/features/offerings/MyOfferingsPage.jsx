import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listMyOfferings } from '../../api/offerings'
import { PageHeader } from '../../components/layout/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { OFFERING_SORTABLE } from './offeringRules'
import { OfferingsTable } from './OfferingsTable'
import { SeasonFilter } from './SeasonFilter'

/** /offerings/mine: the offerings the logged-in Field Officer is assigned to (URD FR-OFF-02). */
export function MyOfferingsPage() {
  const list = usePagedList({ sortBy: 'createdAt', direction: 'desc' }, OFFERING_SORTABLE)
  const seasonId = list.filter('seasonId')
  const offerings = useQuery({
    queryKey: ['offerings', 'mine', list.params, seasonId],
    queryFn: () => listMyOfferings(list.params, seasonId ? Number(seasonId) : undefined),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="My assigned offerings"
        description="The offerings you are the Field Officer for: you inspect and score their farmers."
      />
      <div className="mb-4 max-w-xs">
        <SeasonFilter value={seasonId} onChange={(v) => list.setFilter('seasonId', v)} />
      </div>
      <OfferingsTable
        query={offerings}
        sort={list.params}
        onSort={list.toggleSort}
        emptyMessage="You are not assigned to any offering yet."
      />
      <Pagination page={offerings.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
