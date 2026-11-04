import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'
import type { CurrentUser, Permission } from '../types/auth'
import { AuthContext, type AuthState, type LoginNotice } from './AuthContext'
import { clearSession, getToken, saveSession } from './tokenStore'

interface SessionState {
  status: AuthState['status']
  user: CurrentUser | null
  notice: LoginNotice
}

/**
 * Holds the logged-in user for the whole app (URD FR-AUTH-01..07).
 * The backend stays responsible for security; this only decides what the screens show.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<SessionState>(() => ({
    status: getToken() ? 'loading' : 'anonymous',
    user: null,
    notice: null,
  }))

  const endSession = useCallback(
    (notice: LoginNotice) => {
      clearSession()
      queryClient.clear()
      setSession({ status: 'anonymous', user: null, notice })
    },
    [queryClient],
  )

  // Any logged-in call answering 401 means the token expired or was invalidated (FR-AUTH-06).
  useEffect(() => {
    setUnauthorizedHandler(() => endSession('session-expired'))
    return () => setUnauthorizedHandler(null)
  }, [endSession])

  // Start-up: a token survived a page refresh; check it and load the user.
  useEffect(() => {
    if (session.status !== 'loading') return
    let cancelled = false
    authApi
      .getCurrentUser()
      .then((user) => {
        if (!cancelled) setSession({ status: 'authenticated', user, notice: null })
      })
      .catch(() => {
        if (!cancelled) endSession(null)
      })
    return () => {
      cancelled = true
    }
  }, [session.status, endSession])

  const login = useCallback(async (username: string, password: string) => {
    const answer = await authApi.login(username, password)
    saveSession(answer.accessToken, answer.expiresIn)
    try {
      const user = await authApi.getCurrentUser()
      setSession({ status: 'authenticated', user, notice: null })
      return user
    } catch (error) {
      clearSession()
      throw error
    }
  }, [])

  const logout = useCallback(() => endSession(null), [endSession])

  const changePassword = useCallback(
    async (currentPassword: string, newPassword: string) => {
      const username = session.user?.username
      await authApi.changePassword(currentPassword, newPassword)
      // The old token no longer works; log in again with the new password.
      clearSession()
      if (!username) {
        endSession('password-changed')
        return
      }
      try {
        await login(username, newPassword)
      } catch {
        endSession('password-changed')
      }
    },
    [session.user?.username, login, endSession],
  )

  const hasPermission = useCallback(
    (permission: Permission) => session.user?.permissions.includes(permission) ?? false,
    [session.user],
  )

  const value = useMemo<AuthState>(
    () => ({ ...session, login, logout, changePassword, hasPermission }),
    [session, login, logout, changePassword, hasPermission],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
