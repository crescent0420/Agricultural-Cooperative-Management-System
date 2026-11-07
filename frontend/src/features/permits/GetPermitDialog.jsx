import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { toApiError } from '../../api/errors'
import { generatePermit } from '../../api/permits'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Spinner } from '../../components/ui/Spinner'
import { PermitCard } from './PermitCard'

/**
 * FR-PER-01: asks the backend for the permit of one enrolment as soon as it opens, and shows the answer:
 * a new permit, the permit the enrolment already has, or the refusal with its reason. Nothing is checked
 * here first (warnings on attempt): the backend applies the rules and records the attempt.
 */
export function GetPermitDialog({ enrolment, onClose }) {
  const queryClient = useQueryClient()
  const [state, setState] = useState({ status: 'asking' })
  const asked = useRef(false)

  useEffect(() => {
    // Once per opening, even when React runs effects twice in development.
    if (asked.current) return
    asked.current = true
    generatePermit(enrolment.id)
      .then(async ({ permit, created }) => {
        setState({ status: 'done', permit, created })
        await queryClient.invalidateQueries({ queryKey: ['permits'] })
      })
      .catch((error) => setState({ status: 'refused', error: toApiError(error) }))
  }, [enrolment.id, queryClient])

  return (
    <Dialog
      open
      title={`Collection permit: ${enrolment.programmeName}, ${enrolment.seasonName}`}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      {state.status === 'asking' && (
        <div className="py-6 text-center text-brand-700">
          <Spinner label="Asking for the permit" className="size-8" />
        </div>
      )}
      {state.status === 'refused' && <Alert tone="error">{state.error.message}</Alert>}
      {state.status === 'done' && (
        <div className="space-y-4">
          <p className="text-sm">
            {state.created
              ? 'Your permit has been issued. Show this number at the store on the distribution day.'
              : 'You already have this permit for this enrolment. It is the same number as before.'}
          </p>
          <PermitCard permit={state.permit} />
          <Link to="/my/permits" className="inline-block text-sm font-semibold text-brand-700 hover:underline">
            See and print my permits
          </Link>
        </div>
      )}
    </Dialog>
  )
}
