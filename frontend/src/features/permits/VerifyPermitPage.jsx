import { useState } from 'react'
import { toApiError } from '../../api/errors'
import { verifyPermit } from '../../api/permits'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { TextField } from '../../components/ui/TextField'
import { PermitCard } from './PermitCard'
import { normalizePermitNumber, permitNumberProblem } from './permitNumber'

/**
 * /permits/verify: staff check a permit number at the store (URD FR-PER-03). The backend checks it against
 * today's data and lists any problem; the answer is shown large enough to read at arm's length.
 */
export function VerifyPermitPage() {
  const [input, setInput] = useState('')
  const [formatError, setFormatError] = useState(null)
  const [result, setResult] = useState(null) // { verification } | { notFound: number } | { error }
  const [busy, setBusy] = useState(false)

  const check = async (event) => {
    event.preventDefault()
    const number = normalizePermitNumber(input)
    const problem = permitNumberProblem(number)
    setFormatError(problem)
    setResult(null)
    if (problem) return
    setInput(number)
    setBusy(true)
    try {
      setResult({ verification: await verifyPermit(number) })
    } catch (error) {
      const apiError = toApiError(error)
      setResult(apiError.status === 404 ? { notFound: number } : { error: apiError })
    } finally {
      setBusy(false)
    }
  }

  const v = result?.verification
  return (
    <>
      <PageHeader title="Verify a permit" description="Type the number printed on the farmer's permit." />
      <form onSubmit={check} noValidate className="mb-6 flex max-w-xl flex-wrap items-start gap-3">
        <TextField
          label="Permit number"
          className="min-w-64 flex-1"
          placeholder="CP-ABCD-EFGH-JKMN"
          autoComplete="off"
          spellCheck={false}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          error={formatError ?? undefined}
        />
        <Button type="submit" loading={busy} className="sm:mt-6">
          Check
        </Button>
      </form>

      {result?.error && <Alert tone="error">{result.error.message}</Alert>}
      {(v || result?.notFound) && (
        <section
          aria-label="Verification result"
          aria-live="polite"
          className={`mb-6 max-w-3xl rounded-xl border-4 p-6 ${
            v?.valid ? 'border-success bg-brand-50' : 'border-danger bg-danger/5'
          }`}
        >
          <p className={`text-4xl font-extrabold sm:text-5xl ${v?.valid ? 'text-success' : 'text-danger'}`}>
            {v?.valid ? '✓ VALID' : '✕ NOT VALID'}
          </p>
          {result.notFound && <p className="mt-3 text-lg">No permit has the number {result.notFound}.</p>}
          {v && v.problems.length > 0 && (
            <ul aria-label="Problems" className="mt-3 list-disc space-y-1 pl-6 text-lg text-danger">
              {v.problems.map((problem) => (
                <li key={problem}>{problem}</li>
              ))}
            </ul>
          )}
        </section>
      )}
      {v && (
        <div className="max-w-3xl">
          <PermitCard permit={v.permit} />
        </div>
      )}
    </>
  )
}
