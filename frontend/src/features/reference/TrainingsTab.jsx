import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { createTraining, listTrainings, renameTraining } from '../../api/reference'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { NameDialog } from './NameDialog'

const SORTABLE = ['id', 'name', 'createdAt']

/** FR-REF-06: list trainings; the Registrar creates and renames them. */
export function TrainingsTab() {
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENCE_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'name' }, SORTABLE)
  const [dialog, setDialog] = useState(null)

  const trainings = useQuery({
    queryKey: ['trainings', list.params],
    queryFn: () => listTrainings(list.params),
    placeholderData: keepPreviousData,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['trainings'] })
  const create = useMutation({
    mutationFn: (name) => createTraining(name),
    onSuccess: async (t) => {
      await refresh()
      toast.success(`Training ${t.name} created`)
    },
  })
  const rename = useMutation({
    mutationFn: ({ id, name }) => renameTraining(id, name),
    onSuccess: async (t) => {
      await refresh()
      toast.success(`Training renamed to ${t.name}`)
    },
  })

  const columns = [{ header: 'Name', sortField: 'name', cell: (t) => <span className="font-medium">{t.name}</span> }]
  if (canManage) {
    columns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (t) => (
        <Button
          variant="link"
          onClick={() => setDialog({ mode: 'rename', training: t })}
          aria-label={`Rename ${t.name}`}
        >
          Rename
        </Button>
      ),
    })
  }

  return (
    <>
      {canManage && (
        <div className="mb-4 flex justify-end">
          <Button onClick={() => setDialog({ mode: 'create' })}>New training</Button>
        </div>
      )}
      <DataTable
        caption="Trainings"
        columns={columns}
        rows={trainings.data?.content}
        rowKey={(t) => t.id}
        loading={trainings.isPending}
        error={trainings.error}
        emptyMessage="No trainings yet."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={trainings.data} onPage={list.setPage} onSize={list.setSize} />
      {dialog?.mode === 'create' && (
        <NameDialog
          title="New training"
          submitLabel="Create"
          maxLength={150}
          onSubmit={({ name }) => create.mutateAsync(name)}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog?.mode === 'rename' && (
        <NameDialog
          title={`Rename ${dialog.training.name}`}
          submitLabel="Save"
          maxLength={150}
          initial={{ name: dialog.training.name }}
          onSubmit={({ name }) => rename.mutateAsync({ id: dialog.training.id, name })}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  )
}
