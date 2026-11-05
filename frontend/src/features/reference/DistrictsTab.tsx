import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { listDistricts, renameDistrict } from '../../api/reference'
import type { ApiError } from '../../api/errors'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { DataTable, type Column } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Select } from '../../components/ui/Select'
import { usePagedList } from '../../hooks/usePagedList'
import type { District } from '../../types/reference'
import { NameDialog } from './NameDialog'
import { useProvinceOptions } from './useReferenceOptions'

const SORTABLE = ['id', 'name', 'createdAt'] as const

/**
 * URD gap G11: at every start-up the backend matches districts to the official list (seed/rwanda-districts.csv)
 * by exact name. A district renamed away from its official name is kept, and the official one is added again
 * beside it, so a rename should only correct a spelling to the official name.
 */
export const DISTRICT_RENAME_WARNING =
  "District names come from Rwanda's official list. Only correct a spelling to the official name: " +
  'any other name makes the system add the official district again at its next start-up, as a separate, empty district.'

/** FR-REF-01 (list, filter by province) and FR-REF-02 (rename, Registrar). */
export function DistrictsTab() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENCE_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'name' }, SORTABLE)
  const provinceId = list.filter('provinceId')
  const provinces = useProvinceOptions()
  const [renaming, setRenaming] = useState<District | null>(null)

  const districts = useQuery({
    queryKey: ['districts', list.params, provinceId],
    queryFn: () => listDistricts(list.params, provinceId ? Number(provinceId) : undefined),
    placeholderData: keepPreviousData,
  })
  const rename = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => renameDistrict(id, name),
    onSuccess: async (district) => {
      await queryClient.invalidateQueries({ queryKey: ['districts'] })
      toast.success(`District renamed to ${district.name}`)
    },
  })

  const columns: Column<District>[] = [
    { header: 'Name', sortField: 'name', cell: (d) => <span className="font-medium">{d.name}</span> },
    { header: 'Province', cell: (d) => d.provinceName ?? '—' },
  ]
  if (canManage) {
    columns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (d) => (
        <Button variant="link" onClick={() => setRenaming(d)} aria-label={`Rename ${d.name}`}>
          Rename
        </Button>
      ),
    })
  }

  return (
    <>
      <div className="mb-4 max-w-xs">
        <Select label="Province" value={provinceId} onChange={(e) => list.setFilter('provinceId', e.target.value)}>
          <option value="">All provinces</option>
          {provinces.data?.content.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </Select>
      </div>
      <DataTable
        caption="Districts"
        columns={columns}
        rows={districts.data?.content}
        rowKey={(d) => d.id}
        loading={districts.isPending}
        error={districts.error as ApiError | null}
        emptyMessage="No districts match this filter."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={districts.data} onPage={list.setPage} onSize={list.setSize} />
      {renaming && (
        <NameDialog
          title={`Rename ${renaming.name}`}
          submitLabel="Save"
          maxLength={100}
          initial={{ name: renaming.name }}
          warning={DISTRICT_RENAME_WARNING}
          onSubmit={({ name }) => rename.mutateAsync({ id: renaming.id, name })}
          onClose={() => setRenaming(null)}
        />
      )}
    </>
  )
}
