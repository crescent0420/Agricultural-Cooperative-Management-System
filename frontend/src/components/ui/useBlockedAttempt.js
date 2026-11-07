import { useState } from 'react'

const ALLOWED_KEYS = new Set(['Tab', 'Shift', 'Escape'])

/**
 * Project rule (7 Nov 2026): a warning appears when the user tries an action, not before.
 * A field that cannot be used yet (blockedReason set) looks unavailable but says nothing until the user
 * clicks it or presses a key on it; then its yellow warning appears and the attempt is ignored.
 * Tab still moves past it, so keyboard users are never trapped.
 *
 * @param {string | undefined} blockedReason  why the field cannot be used now; undefined = usable
 * @returns {{ blocked: boolean, message: string | undefined, handlers: object }}
 */
export function useBlockedAttempt(blockedReason) {
  const [triedWhile, setTriedWhile] = useState(null)
  const blocked = Boolean(blockedReason)
  const tried = () => setTriedWhile(blockedReason)
  const handlers = blocked
    ? {
        onMouseDown: (event) => {
          event.preventDefault()
          tried()
        },
        onKeyDown: (event) => {
          if (ALLOWED_KEYS.has(event.key)) return
          event.preventDefault()
          tried()
        },
      }
    : {}
  // Shown only for the reason that was in force at the attempt: once the field becomes usable, it disappears.
  return { blocked, message: blocked && triedWhile === blockedReason ? blockedReason : undefined, handlers }
}
