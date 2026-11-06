import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createCooperative, listCooperatives, renameCooperative } from '../../api/reference'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { DistrictFilter } from './DistrictFilter'
import { useDistrictScope } from './useDistrictScope'
import { NameDialog } from './NameDialog'
import { useDistrictOptions } from './useReferenceOptions'

const SORTABLE = ['id', 'name', 'createdAt']

/** FR-REF-03: list cooperatives by district; the Registrar creates and renames them. */
export function CooperativesTab() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENCE_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'name' }, SORTABLE)
  const districtFilter = list.filter('districtId')
  const districtId = useDistrictScope(districtFilter)
  const districts = useDistrictOptions()
  const [dialog, setDialog] = useState(null)

  const cooperatives = useQuery({
    queryKey: ['cooperatives', list.params, districtId],
    queryFn: () => listCooperatives(list.params, districtId),
    placeholderData: keepPreviousData,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['cooperatives'] })
  const create = useMutation({
    mutationFn: ({ name, districtId }) => createCooperative(name, districtId),
    onSuccess: async (c) => {
      await refresh()
      toast.success(`Cooperative ${c.name} created`)
    },
  })
  const rename = useMutation({
    mutationFn: ({ id, name }) => renameCooperative(id, name),
    onSuccess: async (c) => {
      await refresh()
      toast.success(`Cooperative renamed to ${c.name}`)
    },
  })

  const columns = [
    { header: 'Name', sortField: 'name', cell: (c) => <span className="font-medium">{c.name}</span> },
    { header: 'District', cell: (c) => c.districtName },
  ]
  if (canManage) {
    columns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (c) => (
        <Button
          variant="link"
          onClick={() => setDialog({ mode: 'rename', cooperative: c })}
          aria-label={`Rename ${c.name}`}
        >
          Rename
        </Button>
      ),
    })
  }

  return (
    <>
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <DistrictFilter
          value={districtFilter}
          onChange={(v) => list.setFilter('districtId', v)}
          districts={districts.data?.content}
        />
        {canManage && <Button onClick={() => setDialog({ mode: 'create' })}>New cooperative</Button>}
      </div>
      <DataTable
        caption="Cooperatives"
        columns={columns}
        rows={cooperatives.data?.content}
        rowKey={(c) => c.id}
        loading={cooperatives.isPending}
        error={cooperatives.error}
        emptyMessage="No cooperatives yet in this district."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={cooperatives.data} onPage={list.setPage} onSize={list.setSize} />

      {dialog?.mode === 'create' && (
        <NameDialog
          title="New cooperative"
          submitLabel="Create"
          maxLength={150}
          initial={{ districtId: districtFilter }}
          districts={districts.data?.content ?? []}
          onSubmit={({ name, districtId: d }) => create.mutateAsync({ name, districtId: d })}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog?.mode === 'rename' && (
        <NameDialog
          title={`Rename ${dialog.cooperative.name}`}
          submitLabel="Save"
          maxLength={150}
          initial={{ name: dialog.cooperative.name }}
          onSubmit={({ name }) => rename.mutateAsync({ id: dialog.cooperative.id, name })}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}
