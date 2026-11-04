import { NavLink } from 'react-router'
import { useAuth } from '../../auth/useAuth'
import { visibleMenu } from '../../navigation/visibleMenu'

/** The menu for the logged-in user (URD FR-NAV-01). onNavigate closes the phone menu after a click. */
export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { user } = useAuth()
  const groups = visibleMenu(user?.permissions ?? [])

  return (
    <nav aria-label="Main" className="space-y-5 px-3 py-4">
      {groups.map((group) => (
        <div key={group.label ?? 'main'}>
          {group.label && (
            <p className="mb-1 px-3 text-xs font-semibold tracking-wide text-muted uppercase">{group.label}</p>
          )}
          <ul className="space-y-0.5">
            {group.items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive ? 'bg-brand-50 text-brand-700' : 'text-ink hover:bg-surface'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  )
}
