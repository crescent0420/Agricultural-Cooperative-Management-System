import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Alert } from './Alert'
import { Select } from './Select'
import { TextField } from './TextField'

// Project rule (6 Nov 2026): hints grey, warnings yellow, errors red, in fields and in message boxes.
describe('message colours', () => {
  it('shows a field hint grey, a warning yellow and an error red, all read with the field', () => {
    render(
      <TextField label="Hectares" hint="Up to 2 decimals." warning="Locked: farmers enrolled." error="Too large." />,
    )
    const field = screen.getByLabelText('Hectares')
    expect(screen.getByText('Up to 2 decimals.')).toHaveClass('text-muted')
    expect(screen.getByText('Locked: farmers enrolled.').closest('p')).toHaveClass('text-warning')
    expect(screen.getByText('Too large.').closest('p')).toHaveClass('text-danger')
    expect(field).toHaveAccessibleDescription('Up to 2 decimals. Locked: farmers enrolled. Too large.')
    expect(field).toHaveAttribute('aria-invalid', 'true')
  })

  it('gives a dropdown the same yellow warning', () => {
    render(
      <Select label="Programme" warning="Choose a district first.">
        <option value="">All programmes</option>
      </Select>,
    )
    expect(screen.getByText('Choose a district first.').closest('p')).toHaveClass('text-warning')
    expect(screen.getByLabelText('Programme')).toHaveAccessibleDescription('Choose a district first.')
    expect(screen.getByLabelText('Programme')).not.toHaveAttribute('aria-invalid')
  })

  it('colours warning boxes yellow and error boxes red', () => {
    render(
      <>
        <Alert tone="warning">Retirement is final.</Alert>
        <Alert tone="error">Server unreachable.</Alert>
      </>,
    )
    const [warning, error] = screen.getAllByRole('alert')
    expect(warning).toHaveClass('text-warning', 'border-accent-500')
    expect(warning).toHaveTextContent('⚠')
    expect(error).toHaveClass('text-danger')
    expect(error).toHaveTextContent('✕')
  })
})
