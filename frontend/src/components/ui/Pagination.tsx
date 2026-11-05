import type { PagedResponse } from '../../types/api'
import { Button } from './Button'

interface PaginationProps {
  page: PagedResponse<unknown> | undefined
  onPage: (page: number) => void
  onSize: (size: number) => void
}

/** "Page 2 of 5 · 87 records", Previous / Next and rows per page. Pages count from 0 in the API, from 1 on screen. */
export function Pagination({ page, onPage, onSize }: PaginationProps) {
  if (!page || page.totalElements === 0) return null
  return (
    <nav aria-label="Pagination" className="mt-3 flex flex-wrap items-center justify-between gap-3 text-sm text-muted">
      <p>
        Page {page.page + 1} of {Math.max(page.totalPages, 1)} · {page.totalElements}{' '}
        {page.totalElements === 1 ? 'record' : 'records'}
      </p>
      <div className="flex items-center gap-2">
        <label className="flex items-center gap-2">
          Rows
          <select
            value={page.size}
            onChange={(event) => onSize(Number(event.target.value))}
            className="rounded-lg border border-line bg-white px-2 py-1.5 text-ink"
          >
            {[10, 20, 50, 100].map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>
        <Button variant="secondary" disabled={!page.hasPrevious} onClick={() => onPage(page.page - 1)}>
          Previous
        </Button>
        <Button variant="secondary" disabled={!page.hasNext} onClick={() => onPage(page.page + 1)}>
          Next
        </Button>
      </div>
    </nav>
  )
}
