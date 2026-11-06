import { canAccess } from './access'
import { MENU } from './menu'

/** The menu as one user sees it: items they may open, and no empty groups. */
export function visibleMenu(permissions, menu = MENU) {
  const has = (permission) => permissions.includes(permission)
  return menu
    .map((group) => ({ ...group, items: group.items.filter((item) => canAccess(item.requires, has)) }))
    .filter((group) => group.items.length > 0)
}
