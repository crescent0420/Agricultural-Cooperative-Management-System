import { Navigate } from 'react-router'
import { AppShell } from '../components/layout/AppShell'
import { ChangePasswordPage } from '../features/auth/ChangePasswordPage'
import { LoginPage } from '../features/auth/LoginPage'
import { NotLinkedPage } from '../features/auth/NotLinkedPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { NotFoundPage } from '../features/errors/NotFoundPage'
import { HomePage } from '../features/home/HomePage'
import { ModulePlaceholder } from '../features/placeholder/ModulePlaceholder'
import { MENU } from '../navigation/menu'
import { CooperativesTab } from '../features/reference/CooperativesTab'
import { DistrictsTab } from '../features/reference/DistrictsTab'
import { GradingScalePage } from '../features/reference/GradingScalePage'
import { ProgrammeDetailPage } from '../features/reference/ProgrammeDetailPage'
import { ProgrammesTab } from '../features/reference/ProgrammesTab'
import { ReferencePage } from '../features/reference/ReferencePage'
import { TrainingsTab } from '../features/reference/TrainingsTab'
import { FarmerDetailPage } from '../features/farmers/FarmerDetailPage'
import { FarmerDetailsTab } from '../features/farmers/FarmerDetailsTab'
import { FarmerResultsTab } from '../features/farmers/FarmerResultsTab'
import { FarmersListPage } from '../features/farmers/FarmersListPage'
import { FarmerStatusTab } from '../features/farmers/FarmerStatusTab'
import { FarmerTrainingsTab } from '../features/farmers/FarmerTrainingsTab'
import { MissingPrerequisitesTab } from '../features/farmers/MissingPrerequisitesTab'
import { MyProfilePage } from '../features/farmers/MyProfilePage'
import { RegisterFarmerPage } from '../features/farmers/RegisterFarmerPage'
import { RequireAuth } from './RequireAuth'
import { RequirePermission } from './RequirePermission'

/** A page that opens only for users holding every listed permission (the backend still checks each call). */
const guarded = (permissions, element) => (
  <RequirePermission requires={{ allOf: permissions }}>{element}</RequirePermission>
)

/** Module pages already built; every other menu item still opens its placeholder. */
const moduleRoutes = [
  {
    path: '/reference',
    element: (
      <RequirePermission requires={{ allOf: ['REFERENCE_READ'] }}>
        <ReferencePage />
      </RequirePermission>
    ),
    children: [
      { index: true, element: <Navigate to="districts" replace /> },
      { path: 'districts', element: <DistrictsTab /> },
      { path: 'cooperatives', element: <CooperativesTab /> },
      { path: 'programmes', element: <ProgrammesTab /> },
      { path: 'trainings', element: <TrainingsTab /> },
    ],
  },
  {
    path: '/reference/programmes/:id',
    element: (
      <RequirePermission requires={{ allOf: ['REFERENCE_READ'] }}>
        <ProgrammeDetailPage />
      </RequirePermission>
    ),
  },
  { path: '/farmers', element: guarded(['FARMER_LIST'], <FarmersListPage />) },
  { path: '/farmers/new', element: guarded(['FARMER_MANAGE'], <RegisterFarmerPage />) },
  {
    path: '/farmers/:id',
    element: guarded(['FARMER_READ'], <FarmerDetailPage />),
    children: [
      { index: true, element: <Navigate to="details" replace /> },
      { path: 'details', element: <FarmerDetailsTab /> },
      { path: 'status', element: <FarmerStatusTab /> },
      { path: 'trainings', element: guarded(['TRAINING_RECORD_READ'], <FarmerTrainingsTab />) },
      { path: 'prerequisites', element: guarded(['TRAINING_RECORD_READ'], <MissingPrerequisitesTab />) },
      { path: 'results', element: guarded(['RESULT_READ'], <FarmerResultsTab />) },
    ],
  },
  { path: '/my/profile', element: guarded(['PROFILE_READ_OWN'], <MyProfilePage />) },
  {
    path: '/grading-scale',
    element: (
      <RequirePermission requires={{ allOf: ['GRADING_SCALE_READ'] }}>
        <GradingScalePage />
      </RequirePermission>
    ),
  },
]
const builtPaths = new Set(moduleRoutes.map((route) => route.path))

const placeholderRoutes = MENU.flatMap((group) => group.items)
  .filter((item) => item.to !== '/' && !builtPaths.has(item.to))
  .map((item) => ({
    path: item.to,
    element: (
      <RequirePermission requires={item.requires}>
        <ModulePlaceholder title={item.label} arrivesIn={item.arrivesIn} />
      </RequirePermission>
    ),
  }))

/** The route table. Tests render it with a memory router; the app with a browser router (AppRouter). */
export const routes = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      { path: '/change-password', element: <ChangePasswordPage /> },
      { path: '/not-linked', element: <NotLinkedPage /> },
      {
        element: <AppShell />,
        children: [{ path: '/', element: <HomePage /> }, ...moduleRoutes, ...placeholderRoutes],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
