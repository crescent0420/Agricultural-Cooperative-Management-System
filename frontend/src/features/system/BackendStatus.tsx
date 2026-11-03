import { useEffect, useState } from 'react'

type Status =
  | { kind: 'checking' }
  | { kind: 'reachable' }
  | { kind: 'unreachable'; detail: string }

const API_ORIGIN = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * Setup check only (replaced by the real login in feature/frontend-authentication).
 * Calls GET /api/auth/me without a token: a running backend answers 401, which proves
 * that the Vite proxy reaches Spring Boot. Any other answer, or none, means it does not.
 */
export function BackendStatus() {
  const [status, setStatus] = useState<Status>({ kind: 'checking' })

  useEffect(() => {
    let cancelled = false
    fetch(`${API_ORIGIN}/api/auth/me`)
      .then((response) => {
        if (cancelled) return
        setStatus(
          response.status === 401
            ? { kind: 'reachable' }
            : { kind: 'unreachable', detail: `unexpected HTTP ${response.status}` },
        )
      })
      .catch(() => {
        if (!cancelled) setStatus({ kind: 'unreachable', detail: 'no response' })
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (status.kind === 'checking') {
    return <p className="text-muted">Checking the backend connection…</p>
  }
  if (status.kind === 'reachable') {
    return (
      <p className="font-medium text-success">
        Backend reachable: the API answered 401 (login required), as expected.
      </p>
    )
  }
  return (
    <p className="font-medium text-danger">
      Backend not reachable ({status.detail}). Is the API running on port 8081?
    </p>
  )
}
