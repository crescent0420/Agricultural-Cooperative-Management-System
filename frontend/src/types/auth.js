// The backend's authentication DTOs and enums, written as JSDoc so editors can show them.

/**
 * com.acms.enums.Role
 * @typedef {'FARMER' | 'FIELD_OFFICER' | 'DISTRICT_AGRONOMIST' | 'UNION_REGISTRAR' | 'FINANCE_OFFICER' | 'SYSTEM_ADMIN'} Role
 */

/** How each role is named on screen. */
export const ROLE_LABELS = {
  FARMER: 'Farmer',
  FIELD_OFFICER: 'Field Officer',
  DISTRICT_AGRONOMIST: 'District Agronomist',
  UNION_REGISTRAR: 'Union Registrar',
  FINANCE_OFFICER: 'Finance Officer',
  SYSTEM_ADMIN: 'System Administrator',
}

/**
 * One of the 44 permissions of com.acms.security.Permission, e.g. 'FARMER_LIST' or 'REFERENCE_MANAGE'.
 * The full list, and which role holds which, is docs/permission-matrix.md (also GET /api/permissions).
 * @typedef {string} Permission
 */

/**
 * POST /api/auth/login -> data (com.acms.dto.LoginResponse)
 * @typedef {object} LoginResponse
 * @property {string} accessToken
 * @property {string} tokenType
 * @property {number} expiresIn  Seconds until the token expires (30 minutes by default).
 * @property {boolean} mustChangePassword
 */

/**
 * GET /api/auth/me -> data (com.acms.dto.CurrentUserResponse)
 * @typedef {object} CurrentUser
 * @property {number} id
 * @property {string} username
 * @property {string | null} email
 * @property {Role} role
 * @property {boolean} emailVerified
 * @property {boolean} mustChangePassword
 * @property {number | null} districtId
 * @property {number | null} farmerId
 * @property {Permission[]} permissions  Empty while the one-time password must still be changed.
 */
