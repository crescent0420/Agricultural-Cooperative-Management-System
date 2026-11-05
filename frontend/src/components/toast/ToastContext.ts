import { createContext } from 'react'

export interface ToastApi {
  /** A short confirmation that disappears by itself, e.g. "Cooperative created". */
  success: (message: string) => void
}

export const ToastContext = createContext<ToastApi | null>(null)
