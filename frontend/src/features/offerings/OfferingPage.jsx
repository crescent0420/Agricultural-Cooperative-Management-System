import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, NavLink, useParams } from 'react-router'
import { LazyOutlet } from '../../components/layout/LazyOutlet'
import { toApiError } from '../../api/errors'
import { getOffering, updateOffering } from '../../api/offerings'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Spinner } from '../../components/ui/Spinner'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { EditOfferingDialog } from './EditOfferingDialog'

/** The tabs of URD section 15. */
const TABS = [
  { to: '.', label: 'Overview', permission: 'OFFERING_READ' },
  { to: 'enrolments', label: 'Enrolments', permission: 'ENROLMENT_LIST_FOR_OFFERING' },
  { to: 'inspections', label: 'Inspections', permission: 'INSPECTION_READ' },
  { to: 'results', label: 'Results', permission: 'INSPECTION_READ' },
]

/**
 * /offerings/:id: one offering with its tabs (URD FR-OFF-04, FR-OFF-05).
 * The backend answers 403 for an offering outside a district user's own district.
 */
export function OfferingPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const offering = useQuery({ queryKey: ['offerings', 'detail', id], queryFn: () => getOffering(id) })

  const save = useMutation({
    mutationFn: (values) => updateOffering(id, values),
    onSuccess: async (o) => {
      queryClient.setQueryData(['offerings', 'detail', id], o)
      await queryClient.invalidateQueries({ queryKey: ['offerings', 'list'] })
      await queryClient.invalidateQueries({ queryKey: ['offerings', 'mine'] })
      toast.success('Offering saved')
    },
  })

  if (offering.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the offering" className="size-8" />
      </div>
    )
  }
  if (offering.error) {
    const error = toApiError(offering.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const o = offering.data
  return (
    <>
      <Link to="/offerings" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Offerings
      </Link>
      <PageHeader
        title={`${o.programmeName}, ${o.seasonName}`}
        description={`${o.districtName} district`}
        actions={
          hasPermission('OFFERING_MANAGE') && (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
          )
        }
      />
      <nav aria-label="Offering" className="mb-5 flex gap-1 overflow-x-auto border-b border-line">
        {TABS.filter((tab) => hasPermission(tab.permission)).map((tab) => (
          <NavLink
            key={tab.label}
            to={tab.to}
            end
            className={({ isActive }) =>
              `-mb-px border-b-2 px-4 py-2 text-sm font-semibold whitespace-nowrap ${
                isActive ? 'border-brand-600 text-brand-700' : 'border-transparent text-muted hover:text-ink'
              }`
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <LazyOutlet context={o} />
      {editing && <EditOfferingDialog offering={o} onSave={save.mutateAsync} onClose={() => setEditing(false)} />}
    </>
  )
}
