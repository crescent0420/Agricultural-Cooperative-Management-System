/**
 * Where the access token lives (URD SEC-04): sessionStorage, so it survives a page refresh
 * but disappears when the browser tab is closed. Never localStorage, never a cookie.
 * If sessionStorage is unavailable (private mode in some browsers), the token is kept in memory only.
 */
const KEY = 'acms.session'

/**
 * @typedef {object} StoredSession
 * @property {string} token
 * @property {number} expiresAt  Milliseconds since 1970, from the login answer's expiresIn.
 */

/** @type {StoredSession | null} */
let memoryFallback = null

function read() {
  try {
    const raw = sessionStorage.getItem(KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return memoryFallback
  }
}

export function saveSession(token, expiresInSeconds) {
  const session = { token, expiresAt: Date.now() + expiresInSeconds * 1000 }
  memoryFallback = session
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session))
  } catch {
    // memory only
  }
}

/** The token, or null when there is none or it has expired (an expired one is removed). */
export function getToken() {
  const session = read()
  if (!session) return null
  if (Date.now() >= session.expiresAt) {
    clearSession()
    return null
  }
  return session.token
}

export function clearSession() {
  memoryFallback = null
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // nothing stored
  }
}
