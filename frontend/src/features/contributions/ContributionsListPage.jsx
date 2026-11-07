import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createContribution, listContributions } from '../../api/contributions'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { SeasonFilter } from '../offerings/SeasonFilter'
import { CONTRIBUTION_SORTABLE } from './contributionFields'
import { ContributionsTable } from './ContributionsTable'
import { FarmerPicker } from './FarmerPicker'
import { NewContributionDialog } from './NewContributionDialog'

/**
 * /contributions: every farmer's contribution, by farmer and season (URD FR-FIN-01, FR-FIN-02).
 * The chosen farmer is kept in the address (id, name and number), so a reload keeps the filter.
 */
export function ContributionsListPage() {
  const { hasPermission } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'createdAt', direction: 'desc' }, CONTRIBUTION_SORTABLE)
  const [creating, setCreating] = useState(false)
  const farmerId = list.filter('farmerId')
  const seasonId = list.filter('seasonId')
  const farmer = farmerId
    ? { id: Number(farmerId), fullName: list.filter('farmerName'), membershipNumber: list.filter('farmerNumber') }
    : null

  const contributions = useQuery({
    queryKey: ['contributions', 'list', list.params, farmerId, seasonId],
    queryFn: () =>
      listContributions(list.params, {
        farmerId: farmerId ? Number(farmerId) : undefined,
        seasonId: seasonId ? Number(seasonId) : undefined,
      }),
    placeholderData: keepPreviousData,
  })
  const create = useMutation({
    mutationFn: (values) => createContribution(values),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['contributions'] })
      toast.success(`Contribution of ${c.farmerName} for ${c.seasonName} created`)
      navigate(`/contributions/${c.id}`)
    },
  })

  return (
    <>
      <PageHeader
        title="Contributions"
        description="What each farmer owes for a season's inputs, what is paid, and what that allows."
        actions={
          hasPermission('CONTRIBUTION_MANAGE') && <Button onClick={() => setCreating(true)}>New contribution</Button>
        }
      />
      <div className="mb-4 grid gap-3 rounded-lg border border-line bg-white p-4 sm:grid-cols-2">
        <FarmerPicker
          value={farmer}
          onChange={(f) =>
            list.setFilters({
              farmerId: f?.id ?? '',
              farmerName: f?.fullName ?? '',
              farmerNumber: f?.membershipNumber ?? '',
            })
          }
        />
        <SeasonFilter value={seasonId} onChange={(v) => list.setFilter('seasonId', v)} />
      </div>
      <ContributionsTable
        query={contributions}
        sort={list.params}
        onSort={list.toggleSort}
        emptyMessage={farmerId || seasonId ? 'No contributions match these filters.' : 'No contributions recorded yet.'}
      />
      <Pagination page={contributions.data} onPage={list.setPage} onSize={list.setSize} />
      {creating && <NewContributionDialog onCreate={create.mutateAsync} onClose={() => setCreating(false)} />}
    </>
  )
}
