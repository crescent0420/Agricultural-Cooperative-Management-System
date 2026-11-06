import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router'
import { Header } from './Header'
import { Sidebar } from './Sidebar'

/** The frame of every logged-in page: header, sidebar (a slide-in menu on phones) and the page itself. */
export function AppShell() {
  const location = useLocation()
  // The phone menu remembers the page it was opened on, so moving to another page closes it.
  const [openedOn, setOpenedOn] = useState(null)
  const menuOpen = openedOn === location.pathname
  const setMenuOpen = (open) => setOpenedOn(open ? location.pathname : null)

  // Esc closes the phone menu.
  useEffect(() => {
    if (!menuOpen) return
    const onKey = (event) => {
      if (event.key === 'Escape') setOpenedOn(null)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [menuOpen])

  return (
    <div className="min-h-screen">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-lg focus:bg-white focus:px-3 focus:py-2 focus:text-ink"
      >
        Skip to content
      </a>
      <Header menuOpen={menuOpen} onToggleMenu={() => setMenuOpen(!menuOpen)} />

      <div className="flex">
        <aside className="sticky top-[60px] hidden h-[calc(100vh-60px)] w-64 shrink-0 overflow-y-auto border-r border-line bg-white lg:block">
          <Sidebar />
        </aside>

        {menuOpen && (
          <div className="fixed inset-0 z-20 lg:hidden">
            <button
              type="button"
              aria-label="Close menu"
              className="absolute inset-0 bg-ink/40"
              onClick={() => setMenuOpen(false)}
            />
            <div
              id="mobile-menu"
              className="absolute top-[60px] bottom-0 left-0 w-72 overflow-y-auto bg-white shadow-lg"
            >
              <Sidebar onNavigate={() => setMenuOpen(false)} />
            </div>
          </div>
        )}

        <main id="main" className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
