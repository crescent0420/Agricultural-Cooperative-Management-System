import { useLocation } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { ROLE_LABELS } from '../../types/auth'
import { FarmerDashboard } from '../dashboard/FarmerDashboard'
import {
  AdminDashboard,
  AgronomistDashboard,
  FieldOfficerDashboard,
  FinanceDashboard,
  RegistrarDashboard,
} from '../dashboard/StaffDashboards'

const DASHBOARDS = {
  FARMER: FarmerDashboard,
  FIELD_OFFICER: FieldOfficerDashboard,
  DISTRICT_AGRONOMIST: AgronomistDashboard,
  UNION_REGISTRAR: RegistrarDashboard,
  FINANCE_OFFICER: FinanceDashboard,
  SYSTEM_ADMIN: AdminDashboard,
}

/**
 * The start page: the dashboard of the logged-in role (URD §17). There is no dashboard endpoint (G5), so
 * each card is built from requests the role may already make, and loads and fails on its own.
 */
export function HomePage() {
  const { user } = useAuth()
  const location = useLocation()
  const notice = location.state?.notice
  if (!user) return null
  const Dashboard = DASHBOARDS[user.role]

  return (
    <>
      <PageHeader
        title="Dashboard"
        description={`Welcome, ${user.username} · ${ROLE_LABELS[user.role] ?? user.role}`}
      />
      <div className="space-y-4">
        {notice && <Alert tone="success">{notice}</Alert>}
        {Dashboard ? <Dashboard /> : <p className="text-muted">The menu shows what your role can open.</p>}
      </div>
    </>
  )
}
