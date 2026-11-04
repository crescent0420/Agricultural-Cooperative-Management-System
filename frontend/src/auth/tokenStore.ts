/**
 * Where the access token lives (URD SEC-04): sessionStorage, so it survives a page refresh
 * but disappears when the browser tab is closed. Never localStorage, never a cookie.
 * If sessionStorage is unavailable (private mode in some browsers), the token is kept in memory only.
 */
const KEY = 'acms.session'

interface StoredSession {
  token: string
  /** Milliseconds since 1970, from the login answer's expiresIn. */
  expiresAt: number
}

let memoryFallback: StoredSession | null = null

function read(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as StoredSession) : null
  } catch {
    return memoryFallback
  }
}

export function saveSession(token: string, expiresInSeconds: number): void {
  const session: StoredSession = { token, expiresAt: Date.now() + expiresInSeconds * 1000 }
  memoryFallback = session
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session))
  } catch {
    // memory only
  }
}

/** The token, or null when there is none or it has expired (an expired one is removed). */
export function getToken(): string | null {
  const session = read()
  if (!session) return null
  if (Date.now() >= session.expiresAt) {
    clearSession()
    return null
  }
  return session.token
}

export function clearSession(): void {
  memoryFallback = null
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // nothing stored
  }
}
