import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toApiError } from '../../api/errors'
import { addPrerequisite, getProgramme, removePrerequisite, renameProgramme } from '../../api/reference'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { DataTable } from '../../components/ui/DataTable'
import { Select } from '../../components/ui/Select'
import { PageSpinner } from '../../components/ui/Spinner'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { NameDialog } from './NameDialog'
import { useTrainingOptions } from './useReferenceOptions'

/** /reference/programmes/:id: a crop programme and its prerequisite trainings (URD FR-REF-04, FR-REF-05). */
export function ProgrammeDetailPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const canManage = hasPermission('REFERENCE_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const trainings = useTrainingOptions()
  const [selectedTraining, setSelectedTraining] = useState('')
  const [removing, setRemoving] = useState(null)
  const [renaming, setRenaming] = useState(false)
  const [actionError, setActionError] = useState(null)

  const programme = useQuery({ queryKey: ['programmes', 'detail', id], queryFn: () => getProgramme(id) })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['programmes'] })

  const add = useMutation({
    mutationFn: (trainingId) => addPrerequisite(id, trainingId),
    onSuccess: async () => {
      setSelectedTraining('')
      setActionError(null)
      await refresh()
      toast.success('Prerequisite training added')
    },
    onError: (error) => setActionError(toApiError(error)),
  })
  const remove = useMutation({
    mutationFn: (trainingId) => removePrerequisite(id, trainingId),
    onSuccess: async () => {
      setRemoving(null)
      setActionError(null)
      await refresh()
      toast.success('Prerequisite training removed')
    },
    onError: (error) => {
      setRemoving(null)
      setActionError(toApiError(error))
    },
  })
  const rename = useMutation({
    mutationFn: (name) => renameProgramme(id, name),
    onSuccess: async (p) => {
      await refresh()
      toast.success(`Crop programme renamed to ${p.name}`)
    },
  })

  if (programme.isPending) return <PageSpinner label="Loading the crop programme" />
  if (programme.error) {
    const error = toApiError(programme.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const p = programme.data
  const linked = new Set(p.prerequisites.map((t) => t.id))
  const available = (trainings.data?.content ?? []).filter((t) => !linked.has(t.id))

  return (
    <>
      <Link
        to="/reference/programmes"
        className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline"
      >
        ← Crop programmes
      </Link>
      <PageHeader
        title={p.name}
        description={`Crop programme of ${p.districtName} district`}
        actions={
          canManage && (
            <Button variant="secondary" onClick={() => setRenaming(true)}>
              Rename
            </Button>
          )
        }
      />

      <section className="space-y-4">
        <h2 className="text-lg font-semibold">Prerequisite trainings</h2>
        <p className="text-sm text-muted">
          A farmer must have completed all of these trainings to enrol in this programme.
        </p>
        {actionError && <Alert tone="error">{actionError.message}</Alert>}

        {canManage && (
          <form
            className="flex flex-wrap items-end gap-3"
            onSubmit={(event) => {
              event.preventDefault()
              if (selectedTraining) add.mutate(Number(selectedTraining))
            }}
          >
            <Select
              label="Training to add"
              className="min-w-64"
              value={selectedTraining}
              onChange={(e) => setSelectedTraining(e.target.value)}
            >
              <option value="">
                {available.length === 0 ? 'All trainings are already linked' : 'Choose a training…'}
              </option>
              {available.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </Select>
            <Button type="submit" disabled={!selectedTraining} loading={add.isPending}>
              Add training
            </Button>
          </form>
        )}

        <DataTable
          caption="Prerequisite trainings"
          columns={[
            { header: 'Training', cell: (t) => <span className="font-medium">{t.name}</span> },
            ...(canManage
              ? [
                  {
                    header: 'Actions',
                    className: 'text-right',
                    cell: (t) => (
                      <Button variant="link" onClick={() => setRemoving(t)} aria-label={`Remove ${t.name}`}>
                        Remove
                      </Button>
                    ),
                  },
                ]
              : []),
          ]}
          rows={p.prerequisites}
          rowKey={(t) => t.id}
          emptyMessage="No prerequisite trainings: any active farmer may enrol."
        />
      </section>

      <ConfirmDialog
        open={removing !== null}
        title="Remove prerequisite training?"
        confirmLabel="Remove"
        busy={remove.isPending}
        onConfirm={() => removing && remove.mutate(removing.id)}
        onCancel={() => setRemoving(null)}
      >
        Farmers will no longer need <strong>{removing?.name}</strong> to enrol in {p.name}.
      </ConfirmDialog>
      {renaming && (
        <NameDialog
          title={`Rename ${p.name}`}
          submitLabel="Save"
          maxLength={150}
          initial={{ name: p.name }}
          onSubmit={({ name }) => rename.mutateAsync(name)}
          onClose={() => setRenaming(false)}
        />
      )}
    </>
  )
}
