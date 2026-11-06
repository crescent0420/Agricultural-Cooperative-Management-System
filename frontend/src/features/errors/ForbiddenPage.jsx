import { Link } from 'react-router'

/** FR-NAV-03: a page the user's role may not open (typed URL, or a 403 from the API). */
export function ForbiddenPage() {
  return (
    <section className="mx-auto max-w-lg py-12 text-center">
      <p className="text-sm font-semibold text-warning">403</p>
      <h1 className="mt-2 text-2xl font-semibold">Not available to you</h1>
      <p className="mt-2 text-muted">
        Your role does not include access to this page. If you need it for your work, ask the system administrator.
      </p>
      <Link to="/" className="mt-6 inline-block font-semibold text-brand-600 hover:underline">
        Back to the dashboard
      </Link>
    </section>
  )
}
