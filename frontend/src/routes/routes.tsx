import type { RouteObject } from 'react-router'
import { ChangePasswordPage } from '../features/auth/ChangePasswordPage'
import { LoginPage } from '../features/auth/LoginPage'
import { NotLinkedPage } from '../features/auth/NotLinkedPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { NotFoundPage } from '../features/errors/NotFoundPage'
import { HomePage } from '../features/home/HomePage'
import { RequireAuth } from './RequireAuth'

/** The route table. Tests render it with a memory router; the app with a browser router (AppRouter). */
export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/change-password', element: <ChangePasswordPage /> },
      { path: '/not-linked', element: <NotLinkedPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]
