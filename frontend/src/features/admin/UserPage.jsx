import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toApiError } from '../../api/errors'
import { getUser, resetUserPassword, setUserEnabled } from '../../api/users'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { PageSpinner } from '../../components/ui/Spinner'
import { ROLE_LABELS } from '../../types/auth'
import { formatDateTime } from '../../utils/format'
import { useDistrictOptions } from '../reference/useReferenceOptions'
import { AccountBadges, FarmerLink } from './AccountBadges'
import { ChangeRoleDialog } from './ChangeRoleDialog'

// Project rule: the buttons stay; trying them on one's own account shows why, in yellow (the backend says 409).
const OWN_ROLE = 'You cannot change your own role.'
const OWN_DISABLE = 'You cannot disable your own account.'

/** /admin/users/:id: one account and what the administrator can do with it (FR-ADM-03, 04, 05). */
export function UserPage() {
  const id = Number(useParams().id)
  const { user: me } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const districts = useDistrictOptions()
  const account = useQuery({ queryKey: ['users', 'one', id], queryFn: () => getUser(id) })
  const [dialog, setDialog] = useState(null) // 'role' | 'enabled' | 'reset'
  const [warning, setWarning] = useState(null)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  if (account.isPending) return <PageSpinner label="Loading the account" />
  if (account.error) return <Alert tone="error">{account.error.message}</Alert>

  const user = account.data
  const own = me?.id === user.id
  const unlinkedFarmer = user.role === 'FARMER' && user.farmerId == null
  const roleLabel = unlinkedFarmer ? 'Link to a farmer record' : 'Change role'
  const districtName =
    user.districtId == null
      ? '—'
      : (districts.data?.content.find((d) => d.id === user.districtId)?.name ?? `#${user.districtId}`)

  const saved = async (updated, message) => {
    queryClient.setQueryData(['users', 'one', id], updated)
    await queryClient.invalidateQueries({ queryKey: ['users'], exact: false, refetchType: 'none' })
    setDialog(null)
    toast.success(message)
  }
  const open = (name) => {
    setFailure(null)
    setWarning(null)
    if (name === 'role' && own) return setWarning(OWN_ROLE)
    if (name === 'enabled' && own && user.enabled) return setWarning(OWN_DISABLE)
    setDialog(name)
  }
  const run = async (action) => {
    setBusy(true)
    setFailure(null)
    try {
      await action()
    } catch (error) {
      setFailure(toApiError(error))
    } finally {
      setBusy(false)
    }
  }
  const closeDialog = () => {
    setDialog(null)
    setFailure(null)
  }

  return (
    <>
      <Link to="/admin/users" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Users
      </Link>
      <PageHeader title={user.username} description={own ? 'This is your own account.' : undefined} />
      {unlinkedFarmer && (
        <div className="mb-4">
          <Alert tone="info">
            This farmer account is not linked to a farmer record yet, so the farmer cannot use the system. Use{' '}
            <strong>Link to a farmer record</strong>.
          </Alert>
        </div>
      )}
      <section className="mb-4 rounded-lg border border-line bg-white p-6">
        <dl className="grid gap-x-6 gap-y-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
          <Detail label="Email">{user.email ?? '—'}</Detail>
          <Detail label="Role">{ROLE_LABELS[user.role] ?? user.role}</Detail>
          <Detail label="District">{districtName}</Detail>
          <Detail label="Farmer record">
            <FarmerLink user={user} />
          </Detail>
          <Detail label="Account">
            <AccountBadges user={user} />
          </Detail>
          <Detail label="Password">
            {user.mustChangePassword ? 'One-time password, not changed yet' : 'Set by the user'}
          </Detail>
          <Detail label="Created">{formatDateTime(user.createdAt)}</Detail>
          <Detail label="Last changed">{formatDateTime(user.updatedAt)}</Detail>
        </dl>
      </section>
      {warning && (
        <div className="mb-3">
          <Alert tone="warning">{warning}</Alert>
        </div>
      )}
      <div className="flex flex-wrap gap-2">
        <Button onClick={() => open('role')}>{roleLabel}</Button>
        <Button variant="secondary" onClick={() => open('enabled')}>
          {user.enabled ? 'Disable account' : 'Enable account'}
        </Button>
        <Button variant="secondary" onClick={() => open('reset')}>
          Reset password
        </Button>
      </div>

      {dialog === 'role' && (
        <ChangeRoleDialog
          user={user}
          title={roleLabel}
          onClose={closeDialog}
          onChanged={(updated) =>
            saved(
              updated,
              unlinkedFarmer && updated.farmerId != null
                ? `${updated.username} is now linked to Farmer #${updated.farmerId}`
                : `${updated.username} is now ${ROLE_LABELS[updated.role]}`,
            )
          }
        />
      )}
      <ConfirmDialog
        open={dialog === 'enabled'}
        title={user.enabled ? `Disable ${user.username}?` : `Enable ${user.username}?`}
        confirmLabel={user.enabled ? 'Disable' : 'Enable'}
        busy={busy}
        onCancel={closeDialog}
        onConfirm={() =>
          run(async () => {
            const updated = await setUserEnabled(user.id, !user.enabled)
            await saved(updated, `${updated.username} ${updated.enabled ? 'enabled' : 'disabled'}`)
          })
        }
      >
        {failure && (
          <div className="mb-3">
            <Alert tone="error">{failure.message}</Alert>
          </div>
        )}
        {user.enabled
          ? `${user.username} will be refused from their next request and cannot log in until the account is enabled again. Nothing they recorded is deleted.`
          : `${user.username} will be able to log in again.`}
      </ConfirmDialog>
      <ConfirmDialog
        open={dialog === 'reset'}
        title={`Reset ${user.username}'s password?`}
        confirmLabel="Email a new password"
        busy={busy}
        onCancel={closeDialog}
        onConfirm={() =>
          run(async () => {
            const message = await resetUserPassword(user.id)
            await account.refetch()
            setDialog(null)
            toast.success(message)
          })
        }
      >
        {failure && (
          <div className="mb-3">
            <Alert tone="error">{failure.message}</Alert>
          </div>
        )}
        A new one-time password is emailed to {user.email ?? 'the user'}. The current password stops working, and the
        user must choose a new one at the next login.
      </ConfirmDialog>
    </>
  )
}

function Detail({ label, children }) {
  return (
    <div>
      <dt className="text-xs font-medium text-muted">{label}</dt>
      <dd className="mt-0.5 text-ink">{children}</dd>
    </div>
  )
}
