import { Button } from './Button'
import { Dialog } from './Dialog'

/** "Are you sure?" before an action that removes or changes something important (URD NFR-05). */
export function ConfirmDialog({ open, title, children, confirmLabel, busy, onConfirm, onCancel }) {
  return (
    <Dialog
      open={open}
      title={title}
      onClose={onCancel}
      footer={
        <>
          <Button variant="secondary" onClick={onCancel} disabled={busy}>
            Cancel
          </Button>
          <Button onClick={onConfirm} loading={busy}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="text-sm text-ink">{children}</div>
    </Dialog>
  )
}
