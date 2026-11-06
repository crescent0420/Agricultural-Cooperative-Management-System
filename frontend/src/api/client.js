import axios from 'axios'
import { getToken } from '../auth/tokenStore'
import { toApiError } from './errors'

/**
 * The ONLY place that talks HTTP. Pages call the functions in api/<module>.js, which use these helpers.
 * In development the base URL is "/api" on the Vite server, which forwards it to Spring Boot (vite.config.js).
 */
export const apiClient = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL ?? ''}/api`,
  headers: { Accept: 'application/json' },
})

let unauthorizedHandler = null

/** AuthProvider registers what happens when a logged-in call answers 401 (the session has expired). */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

/** Called by the response interceptor; exported for tests. */
export function notifyUnauthorized() {
  unauthorizedHandler?.()
}

apiClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
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
export async function apiGet(url, params) {
  const response = await apiClient.get(url, { params })
  return response.data.data
}

/** POST and return `data` from the envelope (undefined for 204 No Content). */
export async function apiPost(url, body) {
  const response = await apiClient.post(url, body)
  return response.data ? response.data.data : undefined
}

/** PUT and return `data` from the envelope. */
export async function apiPut(url, body) {
  const response = await apiClient.put(url, body)
  return response.data.data
}

/** DELETE and return `data` from the envelope (undefined for 204 No Content). */
export async function apiDelete(url) {
  const response = await apiClient.delete(url)
  return response.data ? response.data.data : undefined
}

/** POST and return the envelope's `message`, for answers that carry no data (e.g. register). */
export async function apiPostForMessage(url, body) {
  const response = await apiClient.post(url, body)
  return response.data.message
}
