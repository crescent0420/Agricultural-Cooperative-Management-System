import { Link } from 'react-router'

/** FR-NAV-04: unknown address. */
export function NotFoundPage() {
  return (
    <main className="mx-auto max-w-md px-4 py-20 text-center">
      <p className="text-sm font-semibold text-brand-600">404</p>
      <h1 className="mt-2 text-2xl font-semibold">Page not found</h1>
      <p className="mt-2 text-muted">The address you opened does not exist in ACMS.</p>
      <Link to="/" className="mt-6 inline-block font-semibold text-brand-600 hover:underline">
        Back to the start page
      </Link>
    </main>
  )
}
