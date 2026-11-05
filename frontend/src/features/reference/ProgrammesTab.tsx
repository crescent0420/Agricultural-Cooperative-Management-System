import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import type { ApiError } from '../../api/errors'
import { createProgramme, listProgrammes } from '../../api/reference'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { DataTable, type Column } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import type { CropProgramme } from '../../types/reference'
import { DistrictFilter } from './DistrictFilter'
import { useDistrictScope } from './useDistrictScope'
import { NameDialog } from './NameDialog'
import { useDistrictOptions } from './useReferenceOptions'

const SORTABLE = ['id', 'name', 'createdAt'] as const

/** FR-REF-04: list crop programmes by district; the Registrar creates them. Rename and prerequisites on the detail page. */
export function ProgrammesTab() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENCE_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'name' }, SORTABLE)
  const districtFilter = list.filter('districtId')
  const districtId = useDistrictScope(districtFilter)
  const districts = useDistrictOptions()
  const [creating, setCreating] = useState(false)

  const programmes = useQuery({
    queryKey: ['programmes', list.params, districtId],
    queryFn: () => listProgrammes(list.params, districtId),
    placeholderData: keepPreviousData,
  })
  const create = useMutation({
    mutationFn: ({ name, districtId }: { name: string; districtId: number }) => createProgramme(name, districtId),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['programmes'] })
      toast.success(`Crop programme ${p.name} created`)
    },
  })

  const columns: Column<CropProgramme>[] = [
    {
      header: 'Name',
      sortField: 'name',
      cell: (p) => (
        <Link to={`/reference/programmes/${p.id}`} className="font-medium text-brand-600 hover:underline">
          {p.name}
        </Link>
      ),
    },
    { header: 'District', cell: (p) => p.districtName },
    {
      header: 'Prerequisite trainings',
      cell: (p) => (p.prerequisites.length === 0 ? 'None' : p.prerequisites.map((t) => t.name).join(', ')),
    },
  ]

  return (
    <>
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <DistrictFilter value={districtFilter} onChange={(v) => list.setFilter('districtId', v)} districts={districts.data?.content} />
        {canManage && <Button onClick={() => setCreating(true)}>New crop programme</Button>}
      </div>
      <DataTable
        caption="Crop programmes"
        columns={columns}
        rows={programmes.data?.content}
        rowKey={(p) => p.id}
        loading={programmes.isPending}
        error={programmes.error as ApiError | null}
        emptyMessage="No crop programmes yet in this district."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={programmes.data} onPage={list.setPage} onSize={list.setSize} />
      {creating && (
        <NameDialog
          title="New crop programme"
          submitLabel="Create"
          maxLength={150}
          initial={{ districtId: districtFilter }}
          districts={districts.data?.content ?? []}
          onSubmit={({ name, districtId: d }) => create.mutateAsync({ name, districtId: d as number })}
          onClose={() => setCreating(false)}
        />
      )}
    </>
  )
}
