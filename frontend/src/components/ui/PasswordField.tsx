import { useState, type ComponentProps } from 'react'
import { TextField } from './TextField'

type PasswordFieldProps = Omit<ComponentProps<typeof TextField>, 'type' | 'trailing'>

/** A password input with a Show / Hide button. */
export function PasswordField(props: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)
  return (
    <TextField
      {...props}
      type={visible ? 'text' : 'password'}
      trailing={
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          className="rounded px-2 py-1 text-xs font-semibold text-brand-600 hover:bg-brand-50"
          aria-label={visible ? `Hide ${props.label.toLowerCase()}` : `Show ${props.label.toLowerCase()}`}
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      }
    />
  )
}
