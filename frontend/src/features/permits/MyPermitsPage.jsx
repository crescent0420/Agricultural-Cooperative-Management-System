import { useQuery } from '@tanstack/react-query'
import { listMyPermits } from '../../api/permits'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Spinner } from '../../components/ui/Spinner'
import { PermitCard } from './PermitCard'

const ALL = { page: 0, size: 100, sortBy: 'issuedAt', direction: 'desc' }

/**
 * /my/permits: the farmer's permits, ready to print (URD FR-PER-02). The print holds the permit cards only: the
 * header, menu, page title and Print button are left out, and a card is never split across two sheets.
 */
export function MyPermitsPage() {
  const permits = useQuery({ queryKey: ['permits', 'mine'], queryFn: () => listMyPermits(ALL) })
  const list = permits.data?.content ?? []
  return (
    <>
      <div className="print:hidden">
        <PageHeader
          title="My permits"
          description="Show the permit number at the store on the distribution day."
          actions={
            list.length > 0 && (
              <span>
                <Button variant="secondary" onClick={() => window.print()}>
                  Print
                </Button>
              </span>
            )
          }
        />
      </div>
      {permits.isPending ? (
        <div className="py-12 text-center text-brand-700">
          <Spinner label="Loading your permits" className="size-8" />
        </div>
      ) : permits.error ? (
        <Alert tone="error">{permits.error.message}</Alert>
      ) : list.length === 0 ? (
        <p className="rounded-lg border border-line bg-white px-4 py-8 text-center text-muted">
          No permits yet. Get one from My enrolments once your contribution is fully paid.
        </p>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2 print:block print:space-y-4 print:*:break-inside-avoid">
          {list.map((p) => (
            <PermitCard key={p.id} permit={p} />
          ))}
        </div>
      )}
    </>
  )
}
