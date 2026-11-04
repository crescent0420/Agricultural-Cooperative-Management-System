interface SpinnerProps {
  label?: string
  className?: string
}

/** A small turning circle; the label is read by screen readers. */
export function Spinner({ label = 'Loading', className = 'size-5' }: SpinnerProps) {
  return (
    <span role="status" className="inline-flex items-center">
      <svg className={`animate-spin ${className}`} viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" className="opacity-25" />
        <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      </svg>
      <span className="sr-only">{label}</span>
    </span>
  )
}

/** Full-screen loading state, e.g. while a stored session is checked. */
export function PageSpinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="flex min-h-screen items-center justify-center text-brand-700">
      <Spinner label={label} className="size-8" />
    </div>
  )
}
