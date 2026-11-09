import { Link, useRouteError } from 'react-router'

/**
 * Shown instead of a page that stopped with an unexpected error, or whose code could not be downloaded
 * (pages load on demand, so a lost connection or a new version on the server can cause this). Found during
 * the end-to-end tests (Phase 6, T3): without it React Router showed its developer screen.
 */
export function PageError() {
  const error = useRouteError()
  const detail = error instanceof Error ? error.message : String(error?.statusText ?? error ?? '')
  return (
    <main className="mx-auto max-w-md px-4 py-20 text-center">
      <p className="text-sm font-semibold text-danger">Error</p>
      <h1 className="mt-2 text-2xl font-semibold">Something went wrong</h1>
      <p className="mt-2 text-muted">
        This page stopped because of an unexpected error. Reload it to try again. If it happens again, tell the system
        administrator what you were doing.
      </p>
      <div className="mt-6 flex justify-center gap-4">
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="rounded-lg bg-brand-700 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-800"
        >
          Reload the page
        </button>
        <Link to="/" className="self-center font-semibold text-brand-600 hover:underline">
          Back to the start page
        </Link>
      </div>
      {detail && (
        <details className="mt-6 text-left text-xs text-muted">
          <summary className="cursor-pointer">Technical details</summary>
          <p className="mt-1 font-mono break-words">{detail}</p>
        </details>
      )}
    </main>
  )
}
