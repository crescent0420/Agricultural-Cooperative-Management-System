import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { listSeasons } from '../../api/seasons'
import { Badge } from '../../components/ui/Badge'
import { formatCount, formatDate } from '../../utils/format'
import { CardList, DashboardCard } from './DashboardCard'

/** The card shows at most this many open seasons; "See all" opens /seasons filtered the same way. */
export const OPEN_SEASONS_SHOWN = 5

/**
 * Registrar: seasons with an open enrolment window, newest first, at most five, with a small name search.
 * Counting and searching are done by the backend (GET /seasons?enrolmentOpen=true&name=…).
 */
export function OpenSeasonsCard() {
  const [text, setText] = useState('')
  const [name, setName] = useState('')
  const seasons = useQuery({
    queryKey: ['seasons', 'open', name],
    queryFn: () =>
      listSeasons(
        { page: 0, size: OPEN_SEASONS_SHOWN, sortBy: 'startsOn', direction: 'desc' },
        { enrolmentOpen: true, name },
      ),
    placeholderData: keepPreviousData,
  })
  const seeAll = `/seasons?enrolment=open${name ? `&name=${encodeURIComponent(name)}` : ''}`

  return (
    <DashboardCard title="Enrolment windows" query={seasons}>
      {(page) => (
        <div className="space-y-3">
          <form
            role="search"
            aria-label="Search open seasons"
            className="flex gap-2"
            onSubmit={(e) => {
              e.preventDefault()
              setName(text.trim())
            }}
          >
            <label htmlFor="open-season-search" className="sr-only">
              Season name
            </label>
            <input
              id="open-season-search"
              type="search"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Search a season…"
              className="min-w-0 flex-1 rounded-lg border border-line px-3 py-1.5 text-sm focus:border-brand-600 focus:outline-none"
            />
            <button
              type="submit"
              className="rounded-lg border border-line px-3 py-1.5 text-sm font-semibold text-brand-700 hover:bg-surface"
            >
              Search
            </button>
          </form>
          <p className="text-sm text-muted">
            {name
              ? `${formatCount(page.totalElements)} open ${page.totalElements === 1 ? 'season matches' : 'seasons match'} “${name}”`
              : `${formatCount(page.totalElements)} ${page.totalElements === 1 ? 'season has' : 'seasons have'} an open enrolment window`}
          </p>
          <CardList
            empty={name ? 'No open season matches this search.' : 'No enrolment window is open.'}
            rows={page.content.map((s) => ({
              key: s.id,
              main: s.name,
              detail: `${formatDate(s.startsOn)} – ${formatDate(s.endsOn)}`,
              to: `/seasons/${s.id}`,
              side: <Badge tone="success">Enrolment open</Badge>,
            }))}
          />
          <Link
            to={page.totalElements > 0 ? seeAll : '/seasons'}
            className="inline-block text-sm font-semibold text-brand-700 hover:underline"
          >
            {page.totalElements > OPEN_SEASONS_SHOWN
              ? `See all ${formatCount(page.totalElements)} →`
              : 'Open seasons →'}
          </Link>
        </div>
      )}
    </DashboardCard>
  )
}
