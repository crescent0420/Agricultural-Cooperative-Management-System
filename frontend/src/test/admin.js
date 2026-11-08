/** Test data shaped like the backend's UserResponse, AuditLogResponse and PermissionMatrixResponse. */

const account = (fields) => ({
  email: `${fields.username}@example.test`,
  enabled: true,
  emailVerified: true,
  mustChangePassword: false,
  districtId: null,
  farmerId: null,
  createdAt: '2026-10-15T08:00:00Z',
  updatedAt: '2026-10-15T08:00:00Z',
  ...fields,
})

export const adminAccount = account({ id: 1, username: 'demo.admin', role: 'SYSTEM_ADMIN' })
export const officerAccount = account({ id: 5, username: 'demo.officer', role: 'FIELD_OFFICER', districtId: 15 })
export const linkedFarmerAccount = account({ id: 20, username: 'uwase', role: 'FARMER', farmerId: 7 })
export const unlinkedFarmerAccount = account({
  id: 22,
  username: 'self.registered',
  role: 'FARMER',
  emailVerified: false,
})

export const auditEvents = [
  {
    id: 301,
    occurredAt: '2026-11-07T09:30:00Z',
    eventType: 'USER_ROLE_CHANGED',
    success: true,
    actorUserId: 1,
    actorUsername: 'demo.admin',
    targetType: 'USER',
    targetId: 22,
    details: 'Role changed from FARMER to FARMER',
    ipAddress: '127.0.0.1',
  },
  {
    id: 300,
    occurredAt: '2026-11-07T09:00:00Z',
    eventType: 'LOGIN_FAILURE',
    success: false,
    actorUserId: null,
    actorUsername: 'unknown.person',
    targetType: null,
    targetId: null,
    details: 'Bad credentials',
    ipAddress: '10.0.0.8',
  },
  {
    id: 299,
    occurredAt: '2026-11-07T08:00:00Z',
    eventType: 'FARMER_STATUS_CHANGED',
    success: true,
    actorUserId: 2,
    actorUsername: 'demo.registrar',
    targetType: 'FARMER',
    targetId: 7,
    details: 'ACTIVE -> SUSPENDED',
    ipAddress: null,
  },
]

const ROLES = ['FARMER', 'FIELD_OFFICER', 'DISTRICT_AGRONOMIST', 'UNION_REGISTRAR', 'FINANCE_OFFICER', 'SYSTEM_ADMIN']
export const permissionMatrix = {
  rows: [
    { permission: 'REFERENCE_READ', group: 'Reference data', description: 'Read reference data', roles: ROLES },
    {
      permission: 'REFERENCE_MANAGE',
      group: 'Reference data',
      description: 'Manage reference data',
      roles: ['UNION_REGISTRAR'],
    },
    {
      permission: 'USER_MANAGE',
      group: 'Administration',
      description: 'Manage user accounts',
      roles: ['SYSTEM_ADMIN'],
    },
  ],
  byRole: {
    FARMER: ['REFERENCE_READ'],
    FIELD_OFFICER: ['REFERENCE_READ'],
    DISTRICT_AGRONOMIST: ['REFERENCE_READ'],
    UNION_REGISTRAR: ['REFERENCE_READ', 'REFERENCE_MANAGE'],
    FINANCE_OFFICER: ['REFERENCE_READ'],
    SYSTEM_ADMIN: ['REFERENCE_READ', 'USER_MANAGE'],
  },
}
