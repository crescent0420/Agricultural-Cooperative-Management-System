import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'

/**
 * Page, size, sort and filters of a list, kept in the address (?page=2&sortBy=name&districtId=5),
 * so a refresh, the Back button and a shared link all show the same page.
 * `sortable` must list only the fields the endpoint accepts (otherwise the backend answers 400).
 */
export function usePagedList(defaults, sortable) {
  const [search, setSearch] = useSearchParams()

  const params = useMemo(() => {
    const sortBy = search.get('sortBy')
    const direction = search.get('direction')
    const size = Number(search.get('size'))
    return {
      page: Math.max(0, Number(search.get('page')) || 0),
      size: [10, 20, 50, 100].includes(size) ? size : (defaults.size ?? 20),
      sortBy: sortBy && sortable.includes(sortBy) ? sortBy : defaults.sortBy,
      direction: direction === 'asc' || direction === 'desc' ? direction : (defaults.direction ?? 'asc'),
    }
  }, [search, sortable, defaults.sortBy, defaults.direction, defaults.size])

  const update = useCallback(
    (changes) => {
      setSearch(
        (current) => {
          const next = new URLSearchParams(current)
          for (const [key, value] of Object.entries(changes)) {
            if (value === null || value === undefined || value === '') next.delete(key)
            else next.set(key, String(value))
          }
          return next
        },
        { replace: true },
      )
    },
    [setSearch],
  )

  const setPage = (page) => update({ page })
  const setSize = (size) => update({ size, page: 0 })
  /** Clicking the sorted column again flips the direction. */
  const toggleSort = (field) =>
    update({
      sortBy: field,
      direction: params.sortBy === field && params.direction === 'asc' ? 'desc' : 'asc',
      page: 0,
    })
  const filter = (name) => search.get(name) ?? ''
  const setFilter = (name, value) => update({ [name]: value, page: 0 })
  /** Several filters at once, e.g. { name: 'Uwase', status: '' } ('' removes a filter). */
  const setFilters = (values) => update({ ...values, page: 0 })

  return { params, setPage, setSize, toggleSort, filter, setFilter, setFilters }
}
