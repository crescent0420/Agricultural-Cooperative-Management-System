import { Navigate } from 'react-router'
import { AppShell } from '../components/layout/AppShell'
import { ChangePasswordPage } from '../features/auth/ChangePasswordPage'
import { LoginPage } from '../features/auth/LoginPage'
import { NotLinkedPage } from '../features/auth/NotLinkedPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { NotFoundPage } from '../features/errors/NotFoundPage'
import { PageError } from '../features/errors/PageError'
import { HomePage } from '../features/home/HomePage'
import { ModulePlaceholder } from '../features/placeholder/ModulePlaceholder'
import { MENU } from '../navigation/menu'
import { REPORT_PERMISSIONS } from '../features/reports/reportTabs'
import { LazyReportsPage } from '../features/reports/LazyReportsPage'
import { RequireAuth } from './RequireAuth'
import { RequirePermission } from './RequirePermission'
import {
  AuditLogPage,
  ContributionPage,
  ContributionsListPage,
  CooperativesTab,
  DistrictsTab,
  EnrolmentPage,
  FarmerDetailPage,
  FarmerDetailsTab,
  FarmerResultsTab,
  FarmerStatusTab,
  FarmerTrainingsTab,
  FarmersListPage,
  GradingScalePage,
  MissingPrerequisitesTab,
  MyContributionsPage,
  MyEnrolmentsPage,
  MyOfferingsPage,
  MyPermitsPage,
  MyProfilePage,
  MyResultsPage,
  NewOfferingPage,
  NewUserPage,
  OfferingEnrolmentsTab,
  OfferingInspectionsTab,
  OfferingOverviewTab,
  OfferingPage,
  OfferingResultsTab,
  OfferingsForMePage,
  OfferingsListPage,
  PermissionMatrixPage,
  PermitAttemptsPage,
  ProgrammeDetailPage,
  ProgrammesTab,
  ReferencePage,
  RegisterFarmerPage,
  ResultPage,
  ScoreEntryPage,
  SeasonPage,
  SeasonsListPage,
  TrainingsTab,
  UserPage,
  UsersListPage,
  VerifyPermitPage,
} from './lazyPages'

/** A page that opens only for users holding every listed permission (the backend still checks each call). */
const guarded = (permissions, element) => (
  <RequirePermission requires={{ allOf: permissions }}>{element}</RequirePermission>
)

const reportsPage = (
  <RequirePermission requires={{ anyOf: REPORT_PERMISSIONS }}>
    <LazyReportsPage />
  </RequirePermission>
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
  { path: '/seasons', element: guarded(['SEASON_READ'], <SeasonsListPage />) },
  { path: '/seasons/:id', element: guarded(['SEASON_READ'], <SeasonPage />) },
  // Staff pages: farmers also hold OFFERING_READ but browse offerings in "Offerings for me" (enrolment module).
  { path: '/offerings', element: guarded(['OFFERING_READ', 'ENROLMENT_LIST_FOR_OFFERING'], <OfferingsListPage />) },
  { path: '/offerings/mine', element: guarded(['OFFERING_READ_ASSIGNED'], <MyOfferingsPage />) },
  { path: '/offerings/new', element: guarded(['OFFERING_MANAGE'], <NewOfferingPage />) },
  {
    path: '/offerings/:id',
    element: guarded(['OFFERING_READ', 'ENROLMENT_LIST_FOR_OFFERING'], <OfferingPage />),
    children: [
      { index: true, element: <OfferingOverviewTab /> },
      { path: 'enrolments', element: guarded(['ENROLMENT_LIST_FOR_OFFERING'], <OfferingEnrolmentsTab />) },
      { path: 'inspections', element: guarded(['INSPECTION_READ'], <OfferingInspectionsTab />) },
      { path: 'results', element: guarded(['INSPECTION_READ'], <OfferingResultsTab />) },
    ],
  },
  { path: '/my/offerings', element: guarded(['ENROLMENT_CREATE'], <OfferingsForMePage />) },
  { path: '/my/enrolments', element: guarded(['ENROLMENT_READ_OWN'], <MyEnrolmentsPage />) },
  { path: '/enrolments/:id', element: guarded(['ENROLMENT_READ'], <EnrolmentPage />) },
  { path: '/enrolments/:id/scores', element: guarded(['RESULT_ENTER'], <ScoreEntryPage />) },
  { path: '/results/:id', element: guarded(['RESULT_READ'], <ResultPage />) },
  { path: '/my/results', element: guarded(['RESULT_READ_OWN'], <MyResultsPage />) },
  { path: '/contributions', element: guarded(['CONTRIBUTION_LIST'], <ContributionsListPage />) },
  { path: '/contributions/:id', element: guarded(['CONTRIBUTION_READ'], <ContributionPage />) },
  { path: '/my/permits', element: guarded(['PERMIT_READ_OWN'], <MyPermitsPage />) },
  { path: '/permits/verify', element: guarded(['PERMIT_VERIFY'], <VerifyPermitPage />) },
  { path: '/permits/attempts', element: guarded(['PERMIT_ATTEMPT_READ'], <PermitAttemptsPage />) },
  { path: '/my/contributions', element: guarded(['CONTRIBUTION_READ_OWN'], <MyContributionsPage />) },
  { path: '/reports', element: reportsPage },
  { path: '/reports/:report', element: reportsPage },
  { path: '/admin/users', element: guarded(['USER_MANAGE'], <UsersListPage />) },
  { path: '/admin/users/new', element: guarded(['USER_MANAGE'], <NewUserPage />) },
  { path: '/admin/users/:id', element: guarded(['USER_MANAGE'], <UserPage />) },
  { path: '/admin/audit-log', element: guarded(['AUDIT_LOG_READ'], <AuditLogPage />) },
  { path: '/permissions', element: guarded(['PERMISSION_MATRIX_READ'], <PermissionMatrixPage />) },
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
  {
    // Anything else that crashes (outside the shell) shows PageError on its own.
    errorElement: <PageError />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      {
        element: <RequireAuth />,
        children: [
          { path: '/change-password', element: <ChangePasswordPage /> },
          { path: '/not-linked', element: <NotLinkedPage /> },
          {
            element: <AppShell />,
            // A page that crashes shows PageError inside the shell, so the menu stays usable.
            children: [
              {
                errorElement: <PageError />,
                children: [{ path: '/', element: <HomePage /> }, ...moduleRoutes, ...placeholderRoutes],
              },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
