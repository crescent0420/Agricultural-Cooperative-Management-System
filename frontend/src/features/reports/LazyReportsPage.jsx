import { lazy, Suspense } from 'react'
import { Spinner } from '../../components/ui/Spinner'

// The reports page carries the chart library, so it is downloaded only when a user opens it (R-1).
const ReportsPage = lazy(() => import('./ReportsPage'))

export function LazyReportsPage() {
  return (
    <Suspense
      fallback={
        <div className="py-10 text-center text-brand-700">
          <Spinner label="Loading reports" />
        </div>
      }
    >
      <ReportsPage />
    </Suspense>
  )
}
