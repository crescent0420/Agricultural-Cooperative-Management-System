import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listUsers } from '../../api/users'
import { PageHeader } from '../../components/layout/PageHeader'
import { DataTable } from '../../components/ui/DataTable'
import { Pagination } from '../../components/ui/Pagination'
import { usePagedList } from '../../hooks/usePagedList'
import { ROLE_LABELS } from '../../types/auth'
import { formatDate } from '../../utils/format'
import { useDistrictOptions } from '../reference/useReferenceOptions'
import { AccountBadges, FarmerLink } from './AccountBadges'

const SORTABLE = ['id', 'username', 'role', 'createdAt']

/** /admin/users: every account, paged and sortable; no search (G10) (FR-ADM-01). */
export function UsersListPage() {
  const list = usePagedList({ sortBy: 'username', direction: 'asc' }, SORTABLE)
  const users = useQuery({
    queryKey: ['users', list.params],
    queryFn: () => listUsers(list.params),
    placeholderData: keepPreviousData,
  })
  const districts = useDistrictOptions()
  const districtName = (id) => (id == null ? '—' : (districts.data?.content.find((d) => d.id === id)?.name ?? `#${id}`))

  return (
    <>
      <PageHeader
        title="Users"
        description="Staff and farmer accounts. Open one to change its role, enable or disable it, or reset its password."
        actions={
          <Link
            to="/admin/users/new"
            className="inline-flex items-center rounded-lg bg-brand-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-800"
          >
            Create account
          </Link>
        }
      />
      <DataTable
        caption="User accounts"
        columns={[
          {
            header: 'Username',
            sortField: 'username',
            cell: (u) => (
              <Link to={`/admin/users/${u.id}`} className="font-medium text-brand-700 hover:underline">
                {u.username}
              </Link>
            ),
          },
          { header: 'Email', cell: (u) => u.email ?? '—' },
          { header: 'Role', sortField: 'role', cell: (u) => ROLE_LABELS[u.role] ?? u.role },
          { header: 'District', cell: (u) => districtName(u.districtId) },
          { header: 'Farmer record', cell: (u) => <FarmerLink user={u} /> },
          { header: 'Account', cell: (u) => <AccountBadges user={u} /> },
          { header: 'Created', sortField: 'createdAt', cell: (u) => formatDate(u.createdAt) },
        ]}
        rows={users.data?.content}
        rowKey={(u) => u.id}
        loading={users.isPending}
        error={users.error}
        emptyMessage="There are no user accounts."
        sort={list.params}
        onSort={list.toggleSort}
      />
      <Pagination page={users.data} onPage={list.setPage} onSize={list.setSize} />
    </>
  )
}
