import { createContext } from 'react'

/**
 * @typedef {object} ToastApi
 * @property {(message: string) => void} success  A short confirmation that disappears by itself, e.g. "Cooperative created".
 */

/** @type {import('react').Context<ToastApi | null>} */
export const ToastContext = createContext(null)
