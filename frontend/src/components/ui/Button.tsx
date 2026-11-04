import type { ComponentProps } from 'react'
import { Spinner } from './Spinner'

type Variant = 'primary' | 'secondary' | 'link'

interface ButtonProps extends ComponentProps<'button'> {
  variant?: Variant
  /** Shows a spinner and blocks clicks while a request runs (prevents double submits). */
  loading?: boolean
  fullWidth?: boolean
}

const VARIANTS: Record<Variant, string> = {
  // brand-700 (not brand-500) keeps white text at 6.5:1 contrast.
  primary: 'bg-brand-700 text-white hover:bg-brand-800 focus-visible:outline-brand-700',
  secondary: 'border border-line bg-white text-ink hover:bg-surface focus-visible:outline-brand-700',
  link: 'text-brand-600 underline-offset-2 hover:underline focus-visible:outline-brand-700',
}

export function Button({
  variant = 'primary',
  loading = false,
  fullWidth = false,
  type = 'button',
  disabled,
  className = '',
  children,
  ...rest
}: ButtonProps) {
  const padding = variant === 'link' ? '' : 'px-4 py-2.5'
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-lg text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60 ${padding} ${VARIANTS[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...rest}
    >
      {loading && <Spinner label="Please wait" className="size-4" />}
      {children}
    </button>
  )
}
