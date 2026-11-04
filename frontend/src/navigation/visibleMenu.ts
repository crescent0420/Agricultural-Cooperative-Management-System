import type { Permission } from '../types/auth'
import { canAccess } from './access'
import { MENU, type MenuGroup } from './menu'

/** The menu as one user sees it: items they may open, and no empty groups. */
export function visibleMenu(permissions: readonly Permission[], menu: MenuGroup[] = MENU): MenuGroup[] {
  const has = (permission: Permission) => permissions.includes(permission)
  return menu
    .map((group) => ({ ...group, items: group.items.filter((item) => canAccess(item.requires, has)) }))
    .filter((group) => group.items.length > 0)
}
