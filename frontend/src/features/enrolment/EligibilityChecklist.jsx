/** The checklist lines: green tick, red cross, or grey dots while a line is still being checked. */
export function EligibilityChecklist({ items }) {
  return (
    <ul aria-label="Eligibility" className="space-y-1.5 text-sm">
      {items.map((item) => (
        <li key={item.label} className="flex items-start gap-2">
          <span
            aria-hidden="true"
            className={`mt-0.5 w-4 shrink-0 text-center font-bold ${
              item.ok === null ? 'text-muted' : item.ok ? 'text-success' : 'text-danger'
            }`}
          >
            {item.ok === null ? '…' : item.ok ? '✓' : '✕'}
          </span>
          <span>
            <span className="sr-only">{item.ok === null ? 'Not known yet: ' : item.ok ? 'Yes: ' : 'No: '}</span>
            <span className="font-medium">{item.label}.</span>{' '}
            <span className={item.ok === false ? 'text-danger' : 'text-muted'}>{item.reason}</span>
          </span>
        </li>
      ))}
    </ul>
  )
}
