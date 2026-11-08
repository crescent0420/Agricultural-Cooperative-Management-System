import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listAuditLogs } from '../../api/audit'
import { PageHeader } from '../../components/layout/PageHeader'
import { Badge } from '../../components/ui/Badge'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../utils/format'
import { humanize } from './accountRules'

const SORTABLE = ['id', 'occurredAt', 'eventType']
const userLink = (id, text) => (
  <Link to={`/admin/users/${id}`} className="font-medium text-brand-700 hover:underline">
    {text}
  </Link>
)

/** /admin/audit-log: the security audit log, newest first; paging and sorting only (G10) (FR-ADM-06). */
export function AuditLogPage() {
  const list = usePagedList({ sortBy: 'occurredAt', direction: 'desc' }, SORTABLE)
  const events = useQuery({
    queryKey: ['audit-logs', list.params],
    queryFn: () => listAuditLogs(list.params),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="Audit log"
        description="Logins, refused requests and every change the backend records. Entries cannot be edited or deleted."
      />
      <DataTable
        caption="Audit log"
        columns={[
          {
            header: 'When',
            sortField: 'occurredAt',
            cell: (e) => <span className="whitespace-nowrap">{formatDateTime(e.occurredAt)}</span>,
          },
          { header: 'Event', sortField: 'eventType', cell: (e) => humanize(e.eventType) },
          {
            header: 'Outcome',
            cell: (e) => (e.success ? <Badge tone="success">Succeeded</Badge> : <Badge tone="danger">Failed</Badge>),
          },
          {
            header: 'By',
            cell: (e) =>
              e.actorUserId != null
                ? userLink(e.actorUserId, e.actorUsername ?? `User #${e.actorUserId}`)
                : (e.actorUsername ?? '—'),
          },
          {
            header: 'Affected',
            cell: (e) =>
              e.targetType === 'USER' && e.targetId != null
                ? userLink(e.targetId, `User #${e.targetId}`)
                : e.targetType
                  ? `${humanize(e.targetType)}${e.targetId != null ? ` #${e.targetId}` : ''}`
                  : '—',
          },
          { header: 'Details', cell: (e) => e.details ?? '—' },
          { header: 'IP address', cell: (e) => <span className="font-mono text-xs">{e.ipAddress ?? '—'}</span> },
        ]}
        rows={events.data?.content}
        rowKey={(e) => e.id}
        loading={events.isPending}
        error={events.error}
        emptyMessage="The audit log is empty."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={events.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
