import { useQuery } from '@tanstack/react-query'
import { Link, NavLink, Outlet, useParams } from 'react-router'
import { toApiError } from '../../api/errors'
import { getFarmer } from '../../api/farmers'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { Spinner } from '../../components/ui/Spinner'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { StatusBadge } from './StatusBadge'

/** The tabs of URD section 15, each shown only to users who may open it. */
const TABS = [
  { to: 'details', label: 'Details', permission: 'FARMER_READ' },
  { to: 'status', label: 'Status', permission: 'FARMER_READ' },
  { to: 'trainings', label: 'Trainings', permission: 'TRAINING_RECORD_READ' },
  { to: 'prerequisites', label: 'Missing prerequisites', permission: 'TRAINING_RECORD_READ' },
  { to: 'results', label: 'Results', permission: 'RESULT_READ' },
]

/**
 * /farmers/:id: one farmer, with tabs (URD FR-FAR-02, 04..09).
 * The backend answers 403 when the farmer is outside the user's district (or is not the logged-in farmer).
 */
export function FarmerDetailPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const farmer = useQuery({ queryKey: ['farmers', 'detail', id], queryFn: () => getFarmer(id) })

  if (farmer.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the farmer" className="size-8" />
      </div>
    )
  }
  if (farmer.error) {
    const error = toApiError(farmer.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const f = farmer.data
  return (
    <>
      {hasPermission('FARMER_LIST') && (
        <Link to="/farmers" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
          ← Farmers
        </Link>
      )}
      <PageHeader
        title={f.fullName}
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm">{f.membershipNumber}</span>
            <span aria-hidden="true">·</span>
            {f.cooperativeName}, {f.districtName}
            <StatusBadge status={f.status} />
          </span>
        }
      />
      <nav aria-label="Farmer" className="mb-5 flex gap-1 overflow-x-auto border-b border-line">
        {TABS.filter((tab) => hasPermission(tab.permission)).map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
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
      <Outlet context={f} />
    </>
  )
}
