import axios from 'axios'
import { getToken } from '../auth/tokenStore'
import type { ApiResponse } from '../types/api'
import { toApiError } from './errors'

/**
 * The ONLY place that talks HTTP. Pages call the functions in api/<module>.ts, which use these helpers.
 * In development the base URL is "/api" on the Vite server, which forwards it to Spring Boot (vite.config.ts).
 */
export const apiClient = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL ?? ''}/api`,
  headers: { Accept: 'application/json' },
})

let unauthorizedHandler: (() => void) | null = null

/** AuthProvider registers what happens when a logged-in call answers 401 (the session has expired). */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

/** Called by the response interceptor; exported for tests. */
export function notifyUnauthorized(): void {
  unauthorizedHandler?.()
}

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const apiError = toApiError(error)
    if (apiError.status === 401 && axios.isAxiosError(error)) {
      const sentToken = Boolean(error.config?.headers?.get?.('Authorization'))
      const isLogin = error.config?.url === '/auth/login'
      // A 401 from login means "wrong username or password", not "session expired".
      if (sentToken && !isLogin) notifyUnauthorized()
    }
    return Promise.reject(apiError)
  },
)

/** GET and return `data` from the envelope. */
export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url, { params })
  return response.data.data as T
}

/** POST and return `data` from the envelope (undefined for 204 No Content). */
export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, body)
  return (response.data ? response.data.data : undefined) as T
}

/** PUT and return `data` from the envelope. */
export async function apiPut<T>(url: string, body?: unknown): Promise<T> {
  const response = await apiClient.put<ApiResponse<T>>(url, body)
  return response.data.data as T
}

/** DELETE and return `data` from the envelope (undefined for 204 No Content). */
export async function apiDelete<T>(url: string): Promise<T> {
  const response = await apiClient.delete<ApiResponse<T>>(url)
  return (response.data ? response.data.data : undefined) as T
}

/** POST and return the envelope's `message`, for answers that carry no data (e.g. register). */
export async function apiPostForMessage(url: string, body?: unknown): Promise<string> {
  const response = await apiClient.post<ApiResponse<null>>(url, body)
  return response.data.message
}
