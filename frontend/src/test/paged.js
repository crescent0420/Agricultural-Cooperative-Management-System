/** A one-page PagedResponse, shaped like the backend's. */
export function paged(content, page = 0, size = 20, totalElements = content.length) {
  const totalPages = Math.max(1, Math.ceil(totalElements / size))
  return {
    content,
    page,
    size,
    totalElements,
    totalPages,
    first: page === 0,
    last: page >= totalPages - 1,
    hasNext: page < totalPages - 1,
    hasPrevious: page > 0,
    sort: 'name: ASC',
  }
}
