import type { ReactNode } from 'react'
import type { ApiError } from '../../api/errors'
import { Alert } from './Alert'
import { Spinner } from './Spinner'

export interface Column<T> {
  header: string
  cell: (row: T) => ReactNode
  /** The backend's sortBy value; only columns the endpoint can sort get one. */
  sortField?: string
  className?: string
}

interface DataTableProps<T> {
  caption: string
  columns: Column<T>[]
  rows: T[] | undefined
  rowKey: (row: T) => string | number
  loading?: boolean
  error?: ApiError | null
  emptyMessage: string
  sort?: { sortBy: string; direction: 'asc' | 'desc' }
  onSort?: (field: string) => void
}

/** A table with loading, empty and error states and sortable headers (URD NFR-05). Scrolls sideways on phones. */
export function DataTable<T>({ caption, columns, rows, rowKey, loading, error, emptyMessage, sort, onSort }: DataTableProps<T>) {
  if (error) return <Alert tone="error">{error.message}</Alert>

  return (
    <div className="overflow-x-auto rounded-lg border border-line bg-white">
      <table className="min-w-full text-left text-sm">
        <caption className="sr-only">{caption}</caption>
        <thead className="border-b border-line bg-surface text-xs font-semibold tracking-wide text-muted uppercase">
          <tr>
            {columns.map((column) => {
              const sorted = column.sortField && sort?.sortBy === column.sortField
              return (
                <th
                  key={column.header}
                  scope="col"
                  className={`px-4 py-3 ${column.className ?? ''}`}
                  aria-sort={sorted ? (sort?.direction === 'asc' ? 'ascending' : 'descending') : undefined}
                >
                  {column.sortField && onSort ? (
                    <button
                      type="button"
                      onClick={() => onSort(column.sortField as string)}
                      className="inline-flex items-center gap-1 uppercase hover:text-ink"
                    >
                      {column.header}
                      <span aria-hidden="true">{sorted ? (sort?.direction === 'asc' ? '▲' : '▼') : '↕'}</span>
                    </button>
                  ) : (
                    column.header
                  )}
                </th>
              )
            })}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {loading ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center text-brand-700">
                <Spinner label="Loading" />
              </td>
            </tr>
          ) : !rows || rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center text-muted">
                {emptyMessage}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)} className="hover:bg-surface/60">
                {columns.map((column) => (
                  <td key={column.header} className={`px-4 py-3 align-middle ${column.className ?? ''}`}>
                    {column.cell(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
