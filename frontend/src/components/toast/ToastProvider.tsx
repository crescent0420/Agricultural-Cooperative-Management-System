import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { ToastContext, type ToastApi } from './ToastContext'

interface Toast {
  id: number
  message: string
}

/** Shows success messages in the bottom corner for 4 seconds; screen readers announce them. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(1)

  const success = useCallback((message: string) => {
    const id = nextId.current++
    setToasts((current) => [...current, { id, message }])
    window.setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), 4000)
  }, [])

  const api = useMemo<ToastApi>(() => ({ success }), [success])

  return (
    <ToastContext value={api}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed right-4 bottom-4 z-50 flex flex-col gap-2">
        {toasts.map((toast) => (
          <div key={toast.id} role="status" className="rounded-lg bg-brand-700 px-4 py-3 text-sm font-medium text-white shadow-lg">
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext>
  )
}
