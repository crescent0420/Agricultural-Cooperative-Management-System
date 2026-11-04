import { Link, useLocation } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { ROLE_LABELS } from '../../types/auth'

/** Temporary start page after login; the app shell and dashboards replace it in later branches. */
export function HomePage() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const notice = (location.state as { notice?: string } | null)?.notice
  if (!user) return null

  return (
    <>
      <header className="bg-brand-700 text-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between gap-4 px-4 py-4">
          <p className="font-semibold">ACMS</p>
          <Button variant="secondary" onClick={logout}>
            Log out
          </Button>
        </div>
      </header>
      <main className="mx-auto max-w-3xl space-y-4 px-4 py-8">
        {notice && <Alert tone="success">{notice}</Alert>}
        <section className="rounded-lg border border-line bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-semibold">Welcome, {user.username}</h1>
          <p className="mt-2 text-muted">
            You are logged in as <strong className="text-ink">{ROLE_LABELS[user.role]}</strong>, with{' '}
            {user.permissions.length} permissions.
          </p>
          <p className="mt-4 text-sm text-muted">The menus for your role arrive in the next part of the build.</p>
          <Link to="/change-password" className="mt-4 inline-block text-sm font-semibold text-brand-600 hover:underline">
            Change password
          </Link>
        </section>
      </main>
    </>
  )
}
