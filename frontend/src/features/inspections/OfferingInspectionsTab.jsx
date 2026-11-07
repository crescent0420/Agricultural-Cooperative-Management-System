import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toApiError } from '../../api/errors'
import { createInspection, deleteInspection, listInspections, updateInspection } from '../../api/inspections'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { DataTable } from '../../components/ui/DataTable'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { useOffering } from '../offerings/useOffering'
import { InspectionDialog } from './InspectionDialog'

const FROZEN =
  'Results of this offering have been submitted, so its inspections can no longer change (they decide the scores).'

/**
 * Offering page, Inspections tab (URD FR-INS-01, 02): the inspections with their weights' total.
 * Only the offering's assigned Field Officer adds, edits or deletes. Once frozen, trying any of these shows
 * why instead (project rule: warnings on attempt).
 */
export function OfferingInspectionsTab() {
  const offering = useOffering()
  const { user, hasPermission } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const canManage = hasPermission('INSPECTION_MANAGE') && user?.id === offering.fieldOfficerId
  const [editing, setEditing] = useState(null) // {} = new, or an inspection
  const [deleting, setDeleting] = useState(null)
  const [deleteError, setDeleteError] = useState(null)
  const [frozenTried, setFrozenTried] = useState(false)

  const key = ['inspections', offering.id]
  const list = useQuery({ queryKey: key, queryFn: () => listInspections(offering.id) })
  const refresh = () => queryClient.invalidateQueries({ queryKey: key })

  const save = useMutation({
    mutationFn: (values) =>
      editing?.id ? updateInspection(editing.id, values) : createInspection(offering.id, values),
    onSuccess: async (saved) => {
      await refresh()
      toast.success(`Inspection ${saved.name} saved`)
    },
  })
  const remove = useMutation({
    mutationFn: (inspection) => deleteInspection(inspection.id),
    onSuccess: async () => {
      const name = deleting.name
      setDeleting(null)
      await refresh()
      toast.success(`Inspection ${name} deleted`)
    },
    onError: (error) => setDeleteError(toApiError(error)),
  })

  if (list.error && toApiError(list.error).status === 403) return <ForbiddenPage />

  const data = list.data
  const tryChange = (action) => () => (data?.frozen ? setFrozenTried(true) : action())
  const freeWeightFor = (inspection) =>
    data ? Number((100 - data.totalWeight + (inspection?.weight ?? 0)).toFixed(2)) : 100

  const columns = [
    { header: 'Inspection', cell: (i) => <span className="font-medium">{i.name}</span> },
    { header: 'Maximum score', className: 'text-right', cell: (i) => i.maxScore },
    { header: 'Weight', className: 'text-right', cell: (i) => `${i.weight}%` },
  ]
  if (canManage) {
    columns.push({
      header: 'Actions',
      className: 'text-right',
      cell: (i) => (
        <span className="inline-flex gap-3">
          <Button variant="link" onClick={tryChange(() => setEditing(i))} aria-label={`Edit ${i.name}`}>
            Edit
          </Button>
          <Button
            variant="link"
            onClick={tryChange(() => {
              setDeleteError(null)
              setDeleting(i)
            })}
            aria-label={`Delete ${i.name}`}
          >
            Delete
          </Button>
        </span>
      ),
    })
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="flex items-center gap-2 text-sm">
          <span>
            Weights total <strong>{data ? `${data.totalWeight}%` : '…'}</strong> of 100%
          </span>
          {data && (data.weightsComplete ? <Badge tone="success">Complete</Badge> : <Badge>Not complete</Badge>)}
          {data?.frozen && <Badge>Frozen</Badge>}
        </p>
        {canManage && <Button onClick={tryChange(() => setEditing({}))}>Add inspection</Button>}
      </div>
      {frozenTried && <Alert tone="warning">{FROZEN}</Alert>}
      <DataTable
        caption="Inspections"
        columns={columns}
        rows={data?.inspections}
        rowKey={(i) => i.id}
        loading={list.isPending}
        error={list.error}
        emptyMessage="No inspections defined yet."
      />
      {editing && (
        <InspectionDialog
          inspection={editing.id ? editing : null}
          freeWeight={freeWeightFor(editing.id ? editing : null)}
          onSave={save.mutateAsync}
          onClose={() => setEditing(null)}
        />
      )}
      <ConfirmDialog
        open={deleting !== null}
        title={`Delete ${deleting?.name ?? ''}?`}
        confirmLabel="Delete"
        busy={remove.isPending}
        onConfirm={() => remove.mutate(deleting)}
        onCancel={() => setDeleting(null)}
      >
        <div className="space-y-3">
          {deleteError && <Alert tone="error">{deleteError.message}</Alert>}
          <p>The inspection is removed from this offering. Its weight becomes free again.</p>
        </div>
      </ConfirmDialog>
    </section>
  )
}
