import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { getDistrict } from '../../api/reference'
import { useAuth } from '../../auth/useAuth'
import { ROLE_LABELS } from '../../types/auth'

interface HeaderProps {
  menuOpen: boolean
  onToggleMenu: () => void
}

/** Top bar: who is logged in (username, role, district) and the account actions (URD FR-NAV-05). */
export function Header({ menuOpen, onToggleMenu }: HeaderProps) {
  const { user, logout } = useAuth()
  const districtId = user?.districtId ?? null
  const district = useQuery({
    queryKey: ['district', districtId],
    queryFn: () => getDistrict(districtId as number),
    enabled: districtId !== null,
    staleTime: Infinity,
  })
  if (!user) return null

  const roleLine = [ROLE_LABELS[user.role], district.data?.name].filter(Boolean).join(' · ')

  return (
    <header className="sticky top-0 z-30 bg-brand-700 text-white">
      <div className="flex items-center justify-between gap-3 px-4 py-3">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onToggleMenu}
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
            className="rounded-lg p-2 hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-white lg:hidden"
          >
            <span className="sr-only">{menuOpen ? 'Close menu' : 'Open menu'}</span>
            <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              {menuOpen ? <path d="M6 6l12 12M18 6L6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
            </svg>
          </button>
          <Link to="/" className="font-semibold tracking-wide">
            ACMS
          </Link>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right leading-tight">
            <p className="text-sm font-semibold">{user.username}</p>
            <p className="text-xs text-white/85" data-testid="role-line">
              {roleLine}
            </p>
          </div>
          <Link to="/change-password" className="hidden text-sm text-white/90 hover:text-white hover:underline sm:inline">
            Change password
          </Link>
          <button
            type="button"
            onClick={logout}
            className="rounded-lg border border-white/40 px-3 py-1.5 text-sm font-semibold hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-white"
          >
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}
