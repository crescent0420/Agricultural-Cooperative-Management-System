import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toApiError } from '../../api/errors'
import { closeWindow, getSeason, listWindows, openWindow, updateSeason } from '../../api/seasons'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { Spinner } from '../../components/ui/Spinner'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDate, formatDateTime } from '../../utils/format'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { SeasonDialog } from './SeasonDialog'
import { WindowBadge } from './WindowBadge'

/** The fields GET /api/seasons/{id}/windows can sort by (SeasonService.WINDOW_SORT_FIELDS). */
const WINDOW_SORTABLE = ['id', 'openedAt', 'closedAt']

/**
 * /seasons/:id: a season, its enrolment window and the window history (URD FR-SEA-02..04).
 * Backend rules shown here (S1, S2): a season can be edited only until its first window is opened;
 * one window may be open at a time; no window opens after the season has ended.
 */
export function SeasonPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const canManage = hasPermission('SEASON_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'openedAt', direction: 'desc' }, WINDOW_SORTABLE)
  const [editing, setEditing] = useState(false)
  const [windowAction, setWindowAction] = useState(null) // 'open' | 'close'
  const [windowError, setWindowError] = useState(null)

  const season = useQuery({ queryKey: ['seasons', 'detail', id], queryFn: () => getSeason(id) })
  const windows = useQuery({
    queryKey: ['seasons', 'windows', id, list.params],
    queryFn: () => listWindows(id, list.params),
    placeholderData: keepPreviousData,
  })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['seasons'] })

  const save = useMutation({
    mutationFn: (values) => updateSeason(id, values),
    onSuccess: async (s) => {
      queryClient.setQueryData(['seasons', 'detail', id], s)
      await refresh()
      toast.success('Season saved')
    },
  })
  const changeWindow = useMutation({
    mutationFn: (action) => (action === 'open' ? openWindow(id) : closeWindow(id)),
    onSuccess: async (w) => {
      setWindowAction(null)
      await refresh()
      toast.success(w.status === 'OPEN' ? 'Enrolment window opened' : 'Enrolment window closed')
    },
    onError: (error) => setWindowError(toApiError(error)),
  })

  if (season.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the season" className="size-8" />
      </div>
    )
  }
  if (season.error) {
    const error = toApiError(season.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const s = season.data
  // S1: once any window exists (open or closed) the backend refuses edits with 409.
  const hasWindows = (windows.data?.totalElements ?? 0) > 0
  const askWindow = (action) => {
    setWindowError(null)
    setWindowAction(action)
  }

  return (
    <>
      <Link to="/seasons" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Seasons
      </Link>
      <PageHeader
        title={s.name}
        description={<WindowBadge open={s.enrolmentOpen} />}
        actions={
          canManage && (
            <>
              {s.enrolmentOpen ? (
                <Button variant="secondary" onClick={() => askWindow('close')}>
                  Close enrolment window
                </Button>
              ) : (
                <Button onClick={() => askWindow('open')}>Open enrolment window</Button>
              )}
              {!windows.isPending && !hasWindows && (
                <Button variant="secondary" onClick={() => setEditing(true)}>
                  Edit
                </Button>
              )}
            </>
          )
        }
      />

      <section className="mb-6 rounded-lg border border-line bg-white p-6">
        <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { label: 'Starts on', value: formatDate(s.startsOn) },
            { label: 'Ends on', value: formatDate(s.endsOn) },
            { label: 'Land limit per farmer', value: `${s.maxLandHectares} ha` },
            { label: 'Distribution date', value: formatDate(s.distributionDate) },
          ].map(({ label, value }) => (
            <div key={label}>
              <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</dt>
              <dd className="mt-1 text-ink">{value}</dd>
            </div>
          ))}
        </dl>
        {canManage && hasWindows && (
          <p className="mt-4 flex items-start gap-1 text-sm font-medium text-warning">
            <span aria-hidden="true" className="text-accent-500">
              ⚠
            </span>
            These details can no longer be edited: an enrolment window has been opened for this season, and
            farmers&apos; enrolments depend on them.
          </p>
        )}
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Enrolment window history</h2>
        <DataTable
          caption="Enrolment window history"
          columns={[
            {
              header: 'Status',
              cell: (w) => (w.status === 'OPEN' ? <Badge tone="success">Open</Badge> : <Badge>Closed</Badge>),
            },
            { header: 'Opened', sortField: 'openedAt', cell: (w) => formatDateTime(w.openedAt) },
            { header: 'Opened by', cell: (w) => w.openedBy },
            { header: 'Closed', sortField: 'closedAt', cell: (w) => formatDateTime(w.closedAt) },
            { header: 'Closed by', cell: (w) => w.closedBy ?? '—' },
          ]}
          rows={windows.data?.content}
          rowKey={(w) => w.id}
          loading={windows.isPending}
          error={windows.error}
          emptyMessage="No enrolment window has been opened for this season yet."
          sort={list.params}
          onSort={list.toggleSort}
        />
        <Pagination page={windows.data} onPage={list.setPage} onSize={list.setSize} />
      </section>

      <ConfirmDialog
        open={windowAction !== null}
        title={windowAction === 'open' ? `Open enrolment for ${s.name}?` : `Close enrolment for ${s.name}?`}
        confirmLabel={windowAction === 'open' ? 'Open window' : 'Close window'}
        busy={changeWindow.isPending}
        onConfirm={() => changeWindow.mutate(windowAction)}
        onCancel={() => setWindowAction(null)}
      >
        <div className="space-y-3">
          {windowError && <Alert tone="error">{windowError.message}</Alert>}
          {windowAction === 'open' ? (
            <p>
              Farmers will be able to enrol in this season&apos;s offerings until you close the window. After the first
              window is opened, the season&apos;s dates and land limit can no longer be edited.
            </p>
          ) : (
            <p>Farmers will no longer be able to enrol. You can open a new window later while the season lasts.</p>
          )}
        </div>
      </ConfirmDialog>
      {editing && <SeasonDialog season={s} onSave={save.mutateAsync} onClose={() => setEditing(false)} />}
    </>
  )
}
