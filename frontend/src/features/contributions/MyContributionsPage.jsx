import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listMyContributions } from '../../api/contributions'
import { PageHeader } from '../../components/layout/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { CONTRIBUTION_SORTABLE } from './contributionFields'
import { ContributionsTable } from './ContributionsTable'

/** /my/contributions: the farmer's balance for each season, and what it allows (URD FR-FIN-07). */
export function MyContributionsPage() {
  const list = usePagedList({ sortBy: 'createdAt', direction: 'desc' }, CONTRIBUTION_SORTABLE)
  const contributions = useQuery({
    queryKey: ['contributions', 'mine', list.params],
    queryFn: () => listMyContributions(list.params),
    placeholderData: keepPreviousData,
  })
  return (
    <>
      <PageHeader
        title="My contributions"
        description="Paying at least 30% of a season's contribution lets you enrol; paying 100% lets you collect inputs."
      />
      <ContributionsTable
        query={contributions}
        sort={list.params}
        onSort={list.toggleSort}
        showFarmer={false}
        emptyMessage="No contribution has been recorded for you yet. The Finance office records it each season."
      />
      <Pagination page={contributions.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
