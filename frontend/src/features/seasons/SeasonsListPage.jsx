import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { createSeason, listSeasons } from '../../api/seasons'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { TextField } from '../../components/ui/TextField'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDate } from '../../utils/format'
import { SeasonDialog } from './SeasonDialog'
import { WindowBadge } from './WindowBadge'

/** The fields GET /api/seasons can sort by (SeasonService.SEASON_SORT_FIELDS). */
const SORTABLE = ['id', 'name', 'startsOn', 'endsOn', 'createdAt']

/** ?enrolment=open|closed in the address → the backend's enrolmentOpen=true|false. */
const WINDOW_FILTER = { open: true, closed: false }

/** /seasons: every season with its dates, land limit and enrolment window (URD FR-SEA-01, FR-SEA-02). */
export function SeasonsListPage() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('SEASON_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'startsOn', direction: 'desc' }, SORTABLE)
  const [creating, setCreating] = useState(false)
  const name = list.filter('name')
  const enrolment = list.filter('enrolment')
  const filters = { name, enrolmentOpen: WINDOW_FILTER[enrolment] }
  const searching = Boolean(name || enrolment)
  const [text, setText] = useState(name)

  const seasons = useQuery({
    queryKey: ['seasons', 'list', list.params, filters],
    queryFn: () => listSeasons(list.params, filters),
    placeholderData: keepPreviousData,
  })
  const search = (event) => {
    event.preventDefault()
    list.setFilter('name', text.trim())
  }
  const clear = () => {
    setText('')
    list.setFilters({ name: '', enrolment: '' })
  }
  const create = useMutation({
    mutationFn: (values) => createSeason(values),
    onSuccess: async (s) => {
      await queryClient.invalidateQueries({ queryKey: ['seasons'] })
      toast.success(`Season ${s.name} created`)
    },
  })

  return (
    <>
      <PageHeader
        title="Seasons"
        description="Growing seasons, their land limit per farmer and their enrolment windows."
        actions={canManage && <Button onClick={() => setCreating(true)}>New season</Button>}
      />
      <form
        role="search"
        aria-label="Search seasons"
        onSubmit={search}
        className="mb-4 grid gap-3 rounded-lg border border-line bg-white p-4 sm:grid-cols-2 lg:grid-cols-3"
      >
        <TextField label="Season name" placeholder="e.g. 2026" value={text} onChange={(e) => setText(e.target.value)} />
        <Select
          label="Enrolment window"
          value={enrolment}
          onChange={(e) => list.setFilter('enrolment', e.target.value)}
        >
          <option value="">All seasons</option>
          <option value="open">Open</option>
          <option value="closed">Closed</option>
        </Select>
        <div className="flex items-end gap-2">
          <Button type="submit">Search</Button>
          <Button variant="secondary" onClick={clear}>
            Clear
          </Button>
        </div>
      </form>
      <DataTable
        caption="Seasons"
        columns={[
          {
            header: 'Name',
            sortField: 'name',
            cell: (s) => (
              <Link to={`/seasons/${s.id}`} className="font-medium text-brand-700 hover:underline">
                {s.name}
              </Link>
            ),
          },
          { header: 'Starts', sortField: 'startsOn', cell: (s) => formatDate(s.startsOn) },
          { header: 'Ends', sortField: 'endsOn', cell: (s) => formatDate(s.endsOn) },
          { header: 'Land limit (ha)', className: 'text-right', cell: (s) => s.maxLandHectares },
          { header: 'Distribution', cell: (s) => formatDate(s.distributionDate) },
          { header: 'Enrolment', cell: (s) => <WindowBadge open={s.enrolmentOpen} /> },
        ]}
        rows={seasons.data?.content}
        rowKey={(s) => s.id}
        loading={seasons.isPending}
        error={seasons.error}
        emptyMessage={searching ? 'No seasons match this search.' : 'No seasons yet.'}
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={seasons.data} onPage={list.setPage} onSize={list.setSize} />
      {creating && <SeasonDialog onSave={create.mutateAsync} onClose={() => setCreating(false)} />}
    </>
  )
}
