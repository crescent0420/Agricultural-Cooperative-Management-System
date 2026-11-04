import type { Permission } from '../types/auth'

/**
 * What a menu item or page needs. Empty = every logged-in user.
 * allOf: every listed permission; anyOf: at least one of them. Never a role name:
 * the backend's PermissionMatrix decides, and the menu follows it automatically.
 */
export interface Requirement {
  allOf?: Permission[]
  anyOf?: Permission[]
}

export function canAccess(requirement: Requirement, has: (permission: Permission) => boolean): boolean {
  const allOk = (requirement.allOf ?? []).every(has)
  const anyOk = !requirement.anyOf || requirement.anyOf.length === 0 || requirement.anyOf.some(has)
  return allOk && anyOk
}
