import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/errors'
import { DataTable } from './DataTable'

const columns = [
  { header: 'Name', sortField: 'name', cell: (r) => r.name },
  { header: 'Code', cell: (r) => `#${r.id}` },
]
const base = { caption: 'Things', columns, rowKey: (r) => r.id, emptyMessage: 'Nothing here.' }

describe('DataTable', () => {
  it('shows the rows', () => {
    render(<DataTable {...base} rows={[{ id: 1, name: 'Maize' }]} />)
    expect(screen.getByRole('cell', { name: 'Maize' })).toBeInTheDocument()
    expect(screen.getByRole('cell', { name: '#1' })).toBeInTheDocument()
  })

  it('shows loading, empty and error states', () => {
    const { rerender } = render(<DataTable {...base} rows={undefined} loading />)
    expect(screen.getByRole('status')).toHaveTextContent('Loading')
    rerender(<DataTable {...base} rows={[]} />)
    expect(screen.getByText('Nothing here.')).toBeInTheDocument()
    rerender(<DataTable {...base} rows={[]} error={new ApiError(500, 'Server failed')} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Server failed')
  })

  it('sorts only by columns the backend allows, and marks the sorted one', async () => {
    const onSort = vi.fn()
    render(<DataTable {...base} rows={[]} sort={{ sortBy: 'name', direction: 'asc' }} onSort={onSort} />)
    expect(screen.getByRole('columnheader', { name: /Name/ })).toHaveAttribute('aria-sort', 'ascending')
    expect(screen.queryByRole('button', { name: /Code/ })).not.toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: /Name/ }))
    expect(onSort).toHaveBeenCalledWith('name')
  })
})
