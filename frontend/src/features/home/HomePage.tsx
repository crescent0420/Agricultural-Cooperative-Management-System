import { useLocation } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { ROLE_LABELS } from '../../types/auth'

/** The start page inside the app shell; the role dashboards replace its content in the dashboards branch. */
export function HomePage() {
  const { user } = useAuth()
  const location = useLocation()
  const notice = (location.state as { notice?: string } | null)?.notice
  if (!user) return null

  return (
    <>
      <PageHeader title="Dashboard" />
      <div className="space-y-4">
        {notice && <Alert tone="success">{notice}</Alert>}
        <section className="rounded-lg border border-line bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold">Welcome, {user.username}</h2>
          <p className="mt-2 text-muted">
            You are logged in as <strong className="text-ink">{ROLE_LABELS[user.role]}</strong>, with{' '}
            {user.permissions.length} permissions. The menu shows what your role can open.
          </p>
        </section>
      </div>
    </>
  )
}
