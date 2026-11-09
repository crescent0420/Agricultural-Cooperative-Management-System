import { useQuery } from '@tanstack/react-query'
import { Fragment } from 'react'
import { getPermissionMatrix } from '../../api/permissions'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { PageSpinner } from '../../components/ui/Spinner'
import { ROLE_LABELS } from '../../types/auth'

/**
 * /permissions: which role may do what, read-only, exactly as the backend's PermissionMatrix (FR-ADM-07).
 * Grouped by area in the backend's order; the viewer's own role column is highlighted.
 */
export function PermissionMatrixPage() {
  const { user } = useAuth()
  const matrix = useQuery({ queryKey: ['permissions'], queryFn: getPermissionMatrix, staleTime: Infinity })

  if (matrix.isPending) return <PageSpinner label="Loading the permission matrix" />
  if (matrix.error) return <Alert tone="error">{matrix.error.message}</Alert>

  const roles = Object.keys(matrix.data.byRole)
  const groups = []
  for (const row of matrix.data.rows) {
    let group = groups.find((g) => g.name === row.group)
    if (!group) groups.push((group = { name: row.group, rows: [] }))
    group.rows.push(row)
  }
  const mine = (role) => role === user?.role

  return (
    <>
      <PageHeader
        title="Permission matrix"
        description={`What each role may do. Your role, ${ROLE_LABELS[user?.role] ?? ''}, holds ${matrix.data.byRole[user?.role]?.length ?? 0} of ${matrix.data.rows.length} permissions.`}
      />
      {/* relative: the hidden "Yes"/"No" texts are positioned absolutely; without it they escaped the scroll box
          and made the whole page wider than a phone (found by the responsive end-to-end test). */}
      <div className="relative overflow-x-auto rounded-lg border border-line bg-white">
        <table className="min-w-full text-left text-sm">
          <caption className="sr-only">Permission matrix</caption>
          <thead className="border-b border-line bg-surface text-xs font-semibold tracking-wide text-muted uppercase">
            <tr>
              <th scope="col" className="px-4 py-3">
                Permission
              </th>
              {roles.map((role) => (
                <th
                  key={role}
                  scope="col"
                  className={`px-3 py-3 text-center ${mine(role) ? 'bg-brand-50 text-brand-700' : ''}`}
                >
                  {ROLE_LABELS[role] ?? role}
                  {mine(role) && <span className="block text-[10px] normal-case">(you)</span>}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {groups.map((group) => (
              <Fragment key={group.name}>
                <tr className="border-t border-line bg-surface/60">
                  <th scope="rowgroup" colSpan={roles.length + 1} className="px-4 py-2 text-xs font-semibold text-ink">
                    {group.name}
                  </th>
                </tr>
                {group.rows.map((row) => (
                  <tr key={row.permission} className="border-t border-line">
                    <th scope="row" className="px-4 py-2 font-normal">
                      <span className="block text-ink">{row.description}</span>
                      <code className="text-xs text-muted">{row.permission}</code>
                    </th>
                    {roles.map((role) => {
                      const has = row.roles.includes(role)
                      return (
                        <td key={role} className={`px-3 py-2 text-center ${mine(role) ? 'bg-brand-50' : ''}`}>
                          {has ? (
                            <span className="font-semibold text-brand-700">
                              <span aria-hidden="true">✓</span>
                              <span className="sr-only">Yes</span>
                            </span>
                          ) : (
                            <span className="text-muted">
                              <span aria-hidden="true">·</span>
                              <span className="sr-only">No</span>
                            </span>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
