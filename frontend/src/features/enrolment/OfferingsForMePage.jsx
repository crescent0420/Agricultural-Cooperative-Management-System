import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { listMyContributions } from '../../api/contributions'
import { enrol, listMyEnrolments } from '../../api/enrolments'
import { toApiError } from '../../api/errors'
import { getMyProfile } from '../../api/farmers'
import { listOfferings } from '../../api/offerings'
import { listSeasons } from '../../api/seasons'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Pagination } from '../../components/ui/Pagination'
import { Spinner } from '../../components/ui/Spinner'
import { usePagedList } from '../../hooks/usePagedList'
import { OFFERING_SORTABLE } from '../offerings/offeringRules'
import { SeasonFilter } from '../offerings/SeasonFilter'
import { ALL_SEASONS } from '../seasons/useSeasonOptions'
import { OfferingCard } from './OfferingCard'

const ALL = { page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' }
const ALL_ENROLMENTS = { page: 0, size: 100, sortBy: 'enrolledAt', direction: 'desc' }

/**
 * /my/offerings: the offerings of the farmer's district, each with an eligibility checklist and Enrol
 * (URD FR-ENR-01, FR-ENR-02). The backend limits the list to the farmer's district.
 */
export function OfferingsForMePage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const list = usePagedList({ sortBy: 'createdAt', direction: 'desc', size: 10 }, OFFERING_SORTABLE)
  const seasonId = list.filter('seasonId')
  const [choice, setChoice] = useState(null)
  const [enrolError, setEnrolError] = useState(null)

  const offerings = useQuery({
    queryKey: ['offerings', 'for-me', list.params, seasonId],
    queryFn: () => listOfferings(list.params, { seasonId: seasonId ? Number(seasonId) : undefined }),
    placeholderData: keepPreviousData,
  })
  const profile = useQuery({ queryKey: ['me', 'profile'], queryFn: getMyProfile })
  // Fresh (not the 5-minute dropdown cache): whether a window is open matters here.
  const seasons = useQuery({ queryKey: ['seasons', 'for-enrolment'], queryFn: () => listSeasons(ALL_SEASONS) })
  const contributions = useQuery({
    queryKey: ['contributions', 'mine', 'all'],
    queryFn: () => listMyContributions(ALL),
  })
  const enrolments = useQuery({
    queryKey: ['enrolments', 'mine', 'active'],
    queryFn: () => listMyEnrolments(ALL_ENROLMENTS, { status: 'ENROLLED' }),
  })

  const seasonById = new Map((seasons.data?.content ?? []).map((s) => [s.id, s]))
  const contributionFor = (sid) =>
    contributions.data ? (contributions.data.content.find((c) => c.seasonId === sid) ?? null) : undefined
  const enrolledIn = new Set((enrolments.data?.content ?? []).map((e) => e.offeringId))

  const doEnrol = useMutation({
    mutationFn: (offering) => enrol(offering.id),
    onSuccess: async (e) => {
      setChoice(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['enrolments'] }),
        queryClient.invalidateQueries({ queryKey: ['offerings'] }),
      ])
      toast.success(`Enrolled in ${e.programmeName}, ${e.seasonName} (${e.hectares} ha)`)
    },
    onError: (error) => setEnrolError(toApiError(error)),
  })

  const askEnrol = (offering) => {
    setEnrolError(null)
    setChoice(offering)
  }

  return (
    <>
      <PageHeader
        title="Offerings for me"
        description="Crop programmes offered in your district. Check what you need, then enrol."
      />
      <div className="mb-4 max-w-xs">
        <SeasonFilter value={seasonId} onChange={(v) => list.setFilter('seasonId', v)} />
      </div>

      {offerings.isPending ? (
        <div className="py-12 text-center text-brand-700">
          <Spinner label="Loading the offerings" className="size-8" />
        </div>
      ) : offerings.error ? (
        <Alert tone="error">{toApiError(offerings.error).message}</Alert>
      ) : offerings.data.content.length === 0 ? (
        <p className="rounded-lg border border-line bg-white px-4 py-8 text-center text-muted">
          No offerings in your district {seasonId ? 'for this season' : 'yet'}.
        </p>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {offerings.data.content.map((o) => (
            <OfferingCard
              key={o.id}
              offering={o}
              season={seasonById.get(o.seasonId)}
              profile={profile.data}
              contribution={contributionFor(o.seasonId)}
              enrolled={enrolledIn.has(o.id)}
              onEnrol={askEnrol}
            />
          ))}
        </div>
      )}
      <Pagination page={offerings.data} onPage={list.setPage} onSize={list.setSize} />

      <ConfirmDialog
        open={choice !== null}
        title={choice ? `Enrol in ${choice.programmeName}, ${choice.seasonName}?` : ''}
        confirmLabel="Enrol"
        busy={doEnrol.isPending}
        onConfirm={() => doEnrol.mutate(choice)}
        onCancel={() => setChoice(null)}
      >
        <div className="space-y-3">
          {enrolError && <Alert tone="error">{enrolError.message}</Alert>}
          <p>
            You will farm <strong>{choice?.hectaresPerParticipant} ha</strong> under this programme, with Field Officer{' '}
            {choice?.fieldOfficerUsername}. You can cancel while the enrolment window is open.
          </p>
        </div>
      </ConfirmDialog>
    </>
  )
}
