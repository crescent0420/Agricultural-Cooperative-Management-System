import { lazy } from 'react'

// Every module page is downloaded only when it is first opened (decision D4), so the first load stays small.
// The login, start page and shell stay in the main file. Pages with tabs show a spinner where the tab loads.

/** React.lazy for a named export. */
const named = (load, name) => lazy(() => load().then((module) => ({ default: module[name] })))

export const AuditLogPage = named(() => import('../features/admin/AuditLogPage'), 'AuditLogPage')
export const ContributionPage = named(() => import('../features/contributions/ContributionPage'), 'ContributionPage')
export const ContributionsListPage = named(
  () => import('../features/contributions/ContributionsListPage'),
  'ContributionsListPage',
)
export const CooperativesTab = named(() => import('../features/reference/CooperativesTab'), 'CooperativesTab')
export const DistrictsTab = named(() => import('../features/reference/DistrictsTab'), 'DistrictsTab')
export const EnrolmentPage = named(() => import('../features/enrolment/EnrolmentPage'), 'EnrolmentPage')
export const FarmerDetailPage = named(() => import('../features/farmers/FarmerDetailPage'), 'FarmerDetailPage')
export const FarmerDetailsTab = named(() => import('../features/farmers/FarmerDetailsTab'), 'FarmerDetailsTab')
export const FarmerResultsTab = named(() => import('../features/farmers/FarmerResultsTab'), 'FarmerResultsTab')
export const FarmerStatusTab = named(() => import('../features/farmers/FarmerStatusTab'), 'FarmerStatusTab')
export const FarmerTrainingsTab = named(() => import('../features/farmers/FarmerTrainingsTab'), 'FarmerTrainingsTab')
export const FarmersListPage = named(() => import('../features/farmers/FarmersListPage'), 'FarmersListPage')
export const GradingScalePage = named(() => import('../features/reference/GradingScalePage'), 'GradingScalePage')
export const MissingPrerequisitesTab = named(
  () => import('../features/farmers/MissingPrerequisitesTab'),
  'MissingPrerequisitesTab',
)
export const MyContributionsPage = named(
  () => import('../features/contributions/MyContributionsPage'),
  'MyContributionsPage',
)
export const MyEnrolmentsPage = named(() => import('../features/enrolment/MyEnrolmentsPage'), 'MyEnrolmentsPage')
export const MyOfferingsPage = named(() => import('../features/offerings/MyOfferingsPage'), 'MyOfferingsPage')
export const MyPermitsPage = named(() => import('../features/permits/MyPermitsPage'), 'MyPermitsPage')
export const MyProfilePage = named(() => import('../features/farmers/MyProfilePage'), 'MyProfilePage')
export const MyResultsPage = named(() => import('../features/results/MyResultsPage'), 'MyResultsPage')
export const NewOfferingPage = named(() => import('../features/offerings/NewOfferingPage'), 'NewOfferingPage')
export const NewUserPage = named(() => import('../features/admin/NewUserPage'), 'NewUserPage')
export const OfferingEnrolmentsTab = named(
  () => import('../features/enrolment/OfferingEnrolmentsTab'),
  'OfferingEnrolmentsTab',
)
export const OfferingInspectionsTab = named(
  () => import('../features/inspections/OfferingInspectionsTab'),
  'OfferingInspectionsTab',
)
export const OfferingOverviewTab = named(
  () => import('../features/offerings/OfferingOverviewTab'),
  'OfferingOverviewTab',
)
export const OfferingPage = named(() => import('../features/offerings/OfferingPage'), 'OfferingPage')
export const OfferingResultsTab = named(() => import('../features/results/OfferingResultsTab'), 'OfferingResultsTab')
export const OfferingsForMePage = named(() => import('../features/enrolment/OfferingsForMePage'), 'OfferingsForMePage')
export const OfferingsListPage = named(() => import('../features/offerings/OfferingsListPage'), 'OfferingsListPage')
export const PermissionMatrixPage = named(
  () => import('../features/admin/PermissionMatrixPage'),
  'PermissionMatrixPage',
)
export const PermitAttemptsPage = named(() => import('../features/permits/PermitAttemptsPage'), 'PermitAttemptsPage')
export const ProgrammeDetailPage = named(
  () => import('../features/reference/ProgrammeDetailPage'),
  'ProgrammeDetailPage',
)
export const ProgrammesTab = named(() => import('../features/reference/ProgrammesTab'), 'ProgrammesTab')
export const ReferencePage = named(() => import('../features/reference/ReferencePage'), 'ReferencePage')
export const RegisterFarmerPage = named(() => import('../features/farmers/RegisterFarmerPage'), 'RegisterFarmerPage')
export const ResultPage = named(() => import('../features/results/ResultPage'), 'ResultPage')
export const ScoreEntryPage = named(() => import('../features/results/ScoreEntryPage'), 'ScoreEntryPage')
export const SeasonPage = named(() => import('../features/seasons/SeasonPage'), 'SeasonPage')
export const SeasonsListPage = named(() => import('../features/seasons/SeasonsListPage'), 'SeasonsListPage')
export const TrainingsTab = named(() => import('../features/reference/TrainingsTab'), 'TrainingsTab')
export const UserPage = named(() => import('../features/admin/UserPage'), 'UserPage')
export const UsersListPage = named(() => import('../features/admin/UsersListPage'), 'UsersListPage')
export const VerifyPermitPage = named(() => import('../features/permits/VerifyPermitPage'), 'VerifyPermitPage')
