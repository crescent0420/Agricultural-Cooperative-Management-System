import { useState } from 'react'
import { toApiError } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { Select } from '../../components/ui/Select'
import { NEXT_STATUSES, STATUS_LABELS, STATUS_MEANINGS } from './farmerStatus'

/**
 * Change a farmer's status (URD FR-FAR-05). Offers only the changes the backend allows; choosing Retired
 * shows that it is final before anything is saved. A refusal from the backend (422) is shown here.
 */
export function ChangeStatusDialog({ farmer, onSave, onClose }) {
  const options = NEXT_STATUSES[farmer.status] ?? []
  const [status, setStatus] = useState('')
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const save = async (event) => {
    event.preventDefault()
    if (!status) return
    setBusy(true)
    setFailure(null)
    try {
      await onSave(status)
      onClose()
    } catch (error) {
      setFailure(toApiError(error))
      setBusy(false)
    }
  }

  return (
    <Dialog
      open
      title={`Change the status of ${farmer.fullName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button type="submit" form="status-form" disabled={!status} loading={busy}>
            {status === 'RETIRED' ? 'Retire farmer' : 'Change status'}
          </Button>
        </>
      }
    >
      <form id="status-form" onSubmit={save} className="space-y-4">
        {failure && <Alert tone="error">{failure.message}</Alert>}
        <p className="text-sm">
          Current status: <strong>{STATUS_LABELS[farmer.status]}</strong>
        </p>
        <Select label="New status" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">Choose a status…</option>
          {options.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </Select>
        {status && status !== 'RETIRED' && <p className="text-sm text-muted">{STATUS_MEANINGS[status]}</p>}
        {status === 'RETIRED' && (
          <Alert tone="warning">
            Retirement is final. Once {farmer.fullName} is retired, the status can never be changed again.
          </Alert>
        )}
      </form>
    </Dialog>
  )
}
