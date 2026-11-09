import { Suspense } from 'react'
import { Outlet } from 'react-router'
import { Spinner } from '../ui/Spinner'

/**
 * An Outlet whose page may still be downloading (pages are lazy-loaded, decision D4): a spinner shows in
 * its place meanwhile, while the surrounding layout (menu, header, tab bar) stays on screen.
 */
export function LazyOutlet({ context }) {
  return (
    <Suspense
      fallback={
        <div className="py-10 text-center text-brand-700">
          <Spinner label="Loading the page" />
        </div>
      }
    >
      <Outlet context={context} />
    </Suspense>
  )
}
