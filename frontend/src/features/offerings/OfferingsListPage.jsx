import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listOfferings } from '../../api/offerings'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { usePagedList } from '../../hooks/usePagedList'
import { DistrictFilter } from '../reference/DistrictFilter'
import { useDistrictScope } from '../reference/useDistrictScope'
import { useDistrictOptions, useProgrammeOptions } from '../reference/useReferenceOptions'
import { OFFERING_SORTABLE } from './offeringRules'
import { OfferingsTable } from './OfferingsTable'
import { SeasonFilter } from './SeasonFilter'

const toId = (value) => (value ? Number(value) : undefined)

/**
 * /offerings: programme offerings by season, district and programme (URD FR-OFF-01).
 * Field Officers and Agronomists always get their own district: the backend applies it whatever we send.
 */
export function OfferingsListPage() {
  const { hasPermission } = useAuth()
  const list = usePagedList({ sortBy: 'createdAt', direction: 'desc' }, OFFERING_SORTABLE)
  const seasonId = list.filter('seasonId')
  const programmeId = list.filter('programmeId')
  const districtId = useDistrictScope(list.filter('districtId'))
  const districts = useDistrictOptions()
  const programmes = useProgrammeOptions(districtId ?? null)

  const filters = { seasonId: toId(seasonId), districtId, programmeId: toId(programmeId) }
  const offerings = useQuery({
    queryKey: ['offerings', 'list', list.params, filters],
    queryFn: () => listOfferings(list.params, filters),
    placeholderData: keepPreviousData,
  })
  const filtering = Object.values(filters).some((v) => v != null)

  return (
    <>
      <PageHeader
        title="Offerings"
        description="Crop programmes offered in each season, with their Field Officer and places."
        actions={
          hasPermission('OFFERING_MANAGE') && (
            <Link
              to="/offerings/new"
              className="inline-flex items-center justify-center rounded-lg bg-brand-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-800"
            >
              New offering
            </Link>
          )
        }
      />
      <div className="mb-4 grid gap-3 rounded-lg border border-line bg-white p-4 sm:grid-cols-3">
        <SeasonFilter value={seasonId} onChange={(v) => list.setFilter('seasonId', v)} />
        <DistrictFilter
          value={list.filter('districtId')}
          onChange={(v) => list.setFilters({ districtId: v, programmeId: '' })}
          districts={districts.data?.content}
        />
        <Select
          label="Programme"
          value={programmeId}
          disabled={districtId == null}
          hint={districtId == null ? 'Choose a district first.' : undefined}
          onChange={(e) => list.setFilter('programmeId', e.target.value)}
        >
          <option value="">All programmes</option>
          {programmes.data?.content.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </Select>
      </div>
      <OfferingsTable
        query={offerings}
        sort={list.params}
        onSort={list.toggleSort}
        emptyMessage={filtering ? 'No offerings match these filters.' : 'No offerings yet.'}
      />
      <Pagination page={offerings.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
