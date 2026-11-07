import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { searchFarmers } from '../../api/farmers'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDate } from '../../utils/format'
import { DistrictFilter } from '../reference/DistrictFilter'
import { useCooperativeOptions, useDistrictOptions } from '../reference/useReferenceOptions'
import { useDistrictScope } from '../reference/useDistrictScope'
import { FARMER_STATUSES, STATUS_LABELS } from './farmerStatus'
import { StatusBadge } from './StatusBadge'

/** The fields GET /api/farmers can sort by (FarmerService.SORTABLE_FIELDS). */
const SORTABLE = ['id', 'fullName', 'membershipNumber', 'status', 'createdAt']

/**
 * /farmers: search farmers by name, membership number, cooperative and status (URD FR-FAR-01).
 * Field Officers and Agronomists get only their district's farmers: the backend applies that whatever we send.
 * Finance Officers and Administrators may search but not open a farmer (gap G6), so their rows have no link.
 */
export function FarmersListPage() {
  const { hasPermission } = useAuth()
  const canOpen = hasPermission('FARMER_READ')
  const canRegister = hasPermission('FARMER_MANAGE')
  const list = usePagedList({ sortBy: 'fullName' }, SORTABLE)

  const filters = {
    name: list.filter('name'),
    membershipNumber: list.filter('membershipNumber'),
    cooperativeId: list.filter('cooperativeId'),
    status: list.filter('status'),
  }
  // The search endpoint has no district filter; the district only narrows the cooperative dropdown.
  const districtFilter = list.filter('district')
  const districtId = useDistrictScope(districtFilter)
  const districts = useDistrictOptions()
  const cooperatives = useCooperativeOptions(districtId)

  const [text, setText] = useState({ name: filters.name, membershipNumber: filters.membershipNumber })
  const searching = Object.values(filters).some(Boolean)

  const farmers = useQuery({
    queryKey: ['farmers', 'search', list.params, filters],
    queryFn: () =>
      searchFarmers(list.params, {
        ...filters,
        cooperativeId: filters.cooperativeId ? Number(filters.cooperativeId) : undefined,
      }),
    placeholderData: keepPreviousData,
  })

  const applyText = (event) => {
    event.preventDefault()
    list.setFilters({ name: text.name.trim(), membershipNumber: text.membershipNumber.trim() })
  }
  const clearAll = () => {
    setText({ name: '', membershipNumber: '' })
    list.setFilters({ name: '', membershipNumber: '', cooperativeId: '', status: '', district: '' })
  }

  const columns = [
    {
      header: 'Membership no.',
      sortField: 'membershipNumber',
      cell: (f) => <span className="font-mono text-xs">{f.membershipNumber}</span>,
    },
    {
      header: 'Name',
      sortField: 'fullName',
      cell: (f) =>
        canOpen ? (
          <Link to={`/farmers/${f.id}`} className="font-medium text-brand-700 hover:underline">
            {f.fullName}
          </Link>
        ) : (
          <span className="font-medium">{f.fullName}</span>
        ),
    },
    { header: 'Cooperative', cell: (f) => f.cooperativeName },
    { header: 'District', cell: (f) => f.districtName },
    { header: 'Status', sortField: 'status', cell: (f) => <StatusBadge status={f.status} /> },
    { header: 'Registered', sortField: 'createdAt', cell: (f) => formatDate(f.createdAt) },
  ]

  return (
    <>
      <PageHeader
        title="Farmers"
        description="Search the Union's registered farmers."
        actions={
          canRegister && (
            <Link
              to="/farmers/new"
              className="inline-flex items-center justify-center rounded-lg bg-brand-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-800"
            >
              Register farmer
            </Link>
          )
        }
      />

      <form
        role="search"
        aria-label="Search farmers"
        onSubmit={applyText}
        className="mb-4 grid gap-3 rounded-lg border border-line bg-white p-4 sm:grid-cols-2 lg:grid-cols-4"
      >
        <TextField label="Name" value={text.name} onChange={(e) => setText({ ...text, name: e.target.value })} />
        <TextField
          label="Membership number"
          placeholder="RCU-000123"
          value={text.membershipNumber}
          onChange={(e) => setText({ ...text, membershipNumber: e.target.value })}
        />
        <DistrictFilter
          value={districtFilter}
          onChange={(v) => list.setFilters({ district: v, cooperativeId: '' })}
          districts={districts.data?.content}
        />
        <Select
          label="Cooperative"
          value={filters.cooperativeId}
          blockedReason={districtId == null ? 'Choose a district first.' : undefined}
          onChange={(e) => list.setFilter('cooperativeId', e.target.value)}
        >
          <option value="">All cooperatives</option>
          {cooperatives.data?.content.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </Select>
        <Select label="Status" value={filters.status} onChange={(e) => list.setFilter('status', e.target.value)}>
          <option value="">All statuses</option>
          {FARMER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </Select>
        <div className="flex items-end gap-2 lg:col-span-3">
          <Button type="submit">Search</Button>
          <Button variant="secondary" onClick={clearAll}>
            Clear
          </Button>
        </div>
      </form>

      <DataTable
        caption="Farmers"
        columns={columns}
        rows={farmers.data?.content}
        rowKey={(f) => f.id}
        loading={farmers.isPending}
        error={farmers.error}
        emptyMessage={searching ? 'No farmers match this search.' : 'No farmers registered yet.'}
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={farmers.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
