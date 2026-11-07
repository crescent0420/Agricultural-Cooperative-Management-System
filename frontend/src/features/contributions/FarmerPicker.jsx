import { useQuery } from '@tanstack/react-query'
import { useEffect, useId, useState } from 'react'
import { searchFarmers } from '../../api/farmers'
import { Button } from '../../components/ui/Button'
import { FieldMessages } from '../../components/ui/FieldMessages'

const FIRST_TEN = { page: 0, size: 10, sortBy: 'fullName', direction: 'asc' }
const MEMBERSHIP = /^rcu-?\d*$/i

/**
 * Decision P1: contributions are filtered by farmer id, so the user finds the farmer by name or membership
 * number (GET /farmers) and picks one. value: { id, fullName, membershipNumber } or null.
 */
export function FarmerPicker({ label = 'Farmer', value, onChange, error, hint }) {
  const id = useId()
  const [text, setText] = useState('')
  const [term, setTerm] = useState('')

  // Search 300 ms after the last key, and only from 2 characters.
  useEffect(() => {
    const timer = setTimeout(() => setTerm(text.trim()), 300)
    return () => clearTimeout(timer)
  }, [text])

  const filters = MEMBERSHIP.test(term) ? { membershipNumber: term.toUpperCase() } : { name: term }
  const results = useQuery({
    queryKey: ['farmers', 'picker', term],
    queryFn: () => searchFarmers(FIRST_TEN, filters),
    enabled: term.length >= 2 && value == null,
  })

  if (value) {
    return (
      <div>
        <span id={`${id}-label`} className="mb-1 block text-sm font-medium text-ink">
          {label}
        </span>
        <div
          aria-labelledby={`${id}-label`}
          className="flex items-center justify-between gap-3 rounded-lg border border-line bg-white px-3 py-2"
        >
          <span className="text-sm">
            <span className="font-medium">{value.fullName}</span>{' '}
            <span className="font-mono text-xs text-muted">{value.membershipNumber}</span>
          </span>
          <Button variant="link" onClick={() => onChange(null)} aria-label={`Change ${label.toLowerCase()}`}>
            Change
          </Button>
        </div>
        <FieldMessages id={id} hint={hint} error={error} />
      </div>
    )
  }

  const options = results.data?.content ?? []
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-ink">
        {label}
      </label>
      <input
        id={id}
        type="search"
        autoComplete="off"
        placeholder="Name or membership number (RCU-…)"
        value={text}
        onChange={(e) => setText(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
        className={`block w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-ink outline-none placeholder:text-muted focus:border-brand-600 focus:ring-2 focus:ring-brand-200 ${error ? 'border-danger' : 'border-line'}`}
      />
      <FieldMessages id={id} hint={hint} error={error} />
      {term.length >= 2 && (
        <ul
          aria-label={`Farmers matching ${term}`}
          className="mt-1 max-h-60 overflow-y-auto rounded-lg border border-line bg-white text-sm"
        >
          {results.isPending ? (
            <li className="px-3 py-2 text-muted">Searching…</li>
          ) : options.length === 0 ? (
            <li className="px-3 py-2 text-muted">No farmer found.</li>
          ) : (
            options.map((f) => (
              <li key={f.id}>
                <button
                  type="button"
                  onClick={() => {
                    setText('')
                    onChange({ id: f.id, fullName: f.fullName, membershipNumber: f.membershipNumber })
                  }}
                  className="flex w-full items-center justify-between gap-3 px-3 py-2 text-left hover:bg-brand-50 focus:bg-brand-50 focus:outline-none"
                >
                  <span className="font-medium">{f.fullName}</span>
                  <span className="font-mono text-xs text-muted">
                    {f.membershipNumber} · {f.cooperativeName}
                  </span>
                </button>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}
