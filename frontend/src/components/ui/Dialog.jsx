import { useEffect, useId, useRef } from 'react'

/**
 * A modal window: darkened background, Esc closes it, focus moves into it when it opens and
 * returns to the element that opened it when it closes (URD NFR-08).
 *
 * @param {object} props
 * @param {boolean} props.open
 * @param {string} props.title
 * @param {() => void} props.onClose
 * @param {import('react').ReactNode} props.children
 * @param {import('react').ReactNode} [props.footer]  Buttons under the content, e.g. Cancel and Save.
 */
export function Dialog({ open, title, onClose, children, footer }) {
  const titleId = useId()
  const panel = useRef(null)

  useEffect(() => {
    if (!open) return
    const opener = document.activeElement
    const first = panel.current?.querySelector('input, select, textarea, button:not([data-close])')
    ;(first ?? panel.current)?.focus()
    const onKey = (event) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      opener?.focus?.()
    }
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center p-4 sm:items-center">
      <button type="button" aria-label="Close" data-close className="absolute inset-0 bg-ink/40" onClick={onClose} />
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className="relative w-full max-w-lg rounded-lg bg-white p-6 shadow-xl outline-none"
      >
        <h2 id={titleId} className="text-lg font-semibold text-ink">
          {title}
        </h2>
        <div className="mt-4">{children}</div>
        {footer && <div className="mt-6 flex justify-end gap-2">{footer}</div>}
      </div>
    </div>
  )
}
