import type { RouteObject } from 'react-router'
import { AppShell } from '../components/layout/AppShell'
import { ChangePasswordPage } from '../features/auth/ChangePasswordPage'
import { LoginPage } from '../features/auth/LoginPage'
import { NotLinkedPage } from '../features/auth/NotLinkedPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { NotFoundPage } from '../features/errors/NotFoundPage'
import { HomePage } from '../features/home/HomePage'
import { ModulePlaceholder } from '../features/placeholder/ModulePlaceholder'
import { MENU } from '../navigation/menu'
import { RequireAuth } from './RequireAuth'
import { RequirePermission } from './RequirePermission'

/**
 * Module pages not built yet: one placeholder per menu item, guarded by the same permissions as the item.
 * Each feature branch replaces its placeholders with real pages.
 */
const placeholderRoutes: RouteObject[] = MENU.flatMap((group) => group.items)
  .filter((item) => item.to !== '/')
  .map((item) => ({
    path: item.to,
    element: (
      <RequirePermission requires={item.requires}>
        <ModulePlaceholder title={item.label} arrivesIn={item.arrivesIn} />
      </RequirePermission>
    ),
  }))

/** The route table. Tests render it with a memory router; the app with a browser router (AppRouter). */
export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      { path: '/change-password', element: <ChangePasswordPage /> },
      { path: '/not-linked', element: <NotLinkedPage /> },
      {
        element: <AppShell />,
        children: [{ path: '/', element: <HomePage /> }, ...placeholderRoutes],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
