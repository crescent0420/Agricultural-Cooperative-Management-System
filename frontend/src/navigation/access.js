/**
 * What a menu item or page needs. Empty = every logged-in user.
 * allOf: every listed permission; anyOf: at least one of them. Never a role name:
 * the backend's PermissionMatrix decides, and the menu follows it automatically.
 * @typedef {object} Requirement
 * @property {import('../types/auth').Permission[]} [allOf]
 * @property {import('../types/auth').Permission[]} [anyOf]
 */

/**
 * @param {Requirement} requirement
 * @param {(permission: string) => boolean} has
 */
export function canAccess(requirement, has) {
  const allOk = (requirement.allOf ?? []).every(has)
  const anyOk = !requirement.anyOf || requirement.anyOf.length === 0 || requirement.anyOf.some(has)
  return allOk && anyOk
}
