// TypeScript copies of the backend's authentication DTOs and enums.

/** com.acms.enums.Role */
export type Role =
  | 'FARMER'
  | 'FIELD_OFFICER'
  | 'DISTRICT_AGRONOMIST'
  | 'UNION_REGISTRAR'
  | 'FINANCE_OFFICER'
  | 'SYSTEM_ADMIN'

export const ROLE_LABELS: Record<Role, string> = {
  FARMER: 'Farmer',
  FIELD_OFFICER: 'Field Officer',
  DISTRICT_AGRONOMIST: 'District Agronomist',
  UNION_REGISTRAR: 'Union Registrar',
  FINANCE_OFFICER: 'Finance Officer',
  SYSTEM_ADMIN: 'System Administrator',
}

/** com.acms.security.Permission: the 44 permissions, in the backend's order. */
export type Permission =
  | 'REFERENCE_READ'
  | 'REFERENCE_MANAGE'
  | 'SEASON_READ'
  | 'SEASON_MANAGE'
  | 'OFFERING_READ'
  | 'OFFERING_READ_ASSIGNED'
  | 'OFFERING_MANAGE'
  | 'ENROLMENT_CREATE'
  | 'ENROLMENT_READ_OWN'
  | 'ENROLMENT_READ'
  | 'ENROLMENT_LIST_FOR_OFFERING'
  | 'ENROLMENT_CANCEL'
  | 'CONTRIBUTION_READ_OWN'
  | 'CONTRIBUTION_READ'
  | 'CONTRIBUTION_LIST'
  | 'CONTRIBUTION_MANAGE'
  | 'PAYMENT_MANAGE'
  | 'FARMER_LIST'
  | 'FARMER_READ'
  | 'FARMER_MANAGE'
  | 'FARMER_STATUS_CHANGE'
  | 'PROFILE_READ_OWN'
  | 'TRAINING_RECORD_READ'
  | 'TRAINING_RECORD_MANAGE'
  | 'GRADING_SCALE_READ'
  | 'GRADING_SCALE_MANAGE'
  | 'INSPECTION_READ'
  | 'INSPECTION_MANAGE'
  | 'RESULT_ENTER'
  | 'RESULT_APPROVE'
  | 'RESULT_CORRECT'
  | 'RESULT_READ'
  | 'RESULT_READ_OWN'
  | 'PERMIT_GENERATE'
  | 'PERMIT_READ_OWN'
  | 'PERMIT_VERIFY'
  | 'PERMIT_ATTEMPT_READ'
  | 'REPORT_FARMERS_PER_COOPERATIVE'
  | 'REPORT_OFFERING_CAPACITY'
  | 'REPORT_OUTSTANDING_CONTRIBUTIONS'
  | 'REPORT_GRADE_DISTRIBUTION'
  | 'USER_MANAGE'
  | 'AUDIT_LOG_READ'
  | 'PERMISSION_MATRIX_READ'

/** POST /api/auth/login -> data (com.acms.dto.LoginResponse) */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  /** Seconds until the token expires (30 minutes by default). */
  expiresIn: number
  mustChangePassword: boolean
}

/** GET /api/auth/me -> data (com.acms.dto.CurrentUserResponse) */
export interface CurrentUser {
  id: number
  username: string
  email: string | null
  role: Role
  emailVerified: boolean
  mustChangePassword: boolean
  districtId: number | null
  farmerId: number | null
  /** Empty while the one-time password must still be changed. */
  permissions: Permission[]
}
