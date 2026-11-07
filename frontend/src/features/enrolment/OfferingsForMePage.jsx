import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { listMyContributions } from '../../api/contributions'
import { enrol, listMyEnrolments } from '../../api/enrolments'
import { toApiError } from '../../api/errors'
import { getMissingPrerequisites, getMyProfile } from '../../api/farmers'
import { listOfferings } from '../../api/offerings'
import { listSeasons } from '../../api/seasons'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { Spinner } from '../../components/ui/Spinner'
import { useSearchParams } from 'react-router'
import { SeasonFilter } from '../offerings/SeasonFilter'
import { ALL_SEASONS } from '../seasons/useSeasonOptions'
import { eligibilityChecklist, rankOfferings } from './eligibility'
import { OfferingCard } from './OfferingCard'

const ALL = { page: 0, size: 100, sortBy: 'createdAt', direction: 'desc' }

/** Every offering of the farmer's district (100 per request, the API's maximum), so all of them can be ordered. */
async function listAllOfferings(seasonId) {
  const first = await listOfferings(ALL, { seasonId })
  const rest = []
  for (let page = 1; page < first.totalPages; page += 1) {
    rest.push(listOfferings({ ...ALL, page }, { seasonId }))
  }
  return [...first.content, ...(await Promise.all(rest)).flatMap((p) => p.content)]
}
const ALL_ENROLMENTS = { page: 0, size: 100, sortBy: 'enrolledAt', direction: 'desc' }

/**
 * /my/offerings: the offerings of the farmer's district, each with an eligibility checklist and Enrol
 * (URD FR-ENR-01, FR-ENR-02). The backend limits the list to the farmer's district. The offerings whose
 * requirements are all met come first, then by how many are met; enrolled ones last (rankOfferings).
 */
export function OfferingsForMePage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const seasonId = searchParams.get('seasonId') ?? ''
  const [choice, setChoice] = useState(null)
  const [enrolError, setEnrolError] = useState(null)

  const offerings = useQuery({
    queryKey: ['offerings', 'for-me', seasonId],
    queryFn: () => listAllOfferings(seasonId ? Number(seasonId) : undefined),
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

  // One prerequisites request per programme (offerings of the same programme share it), at page level so
  // the checklists are complete before the offerings are ordered.
  const programmeIds = [...new Set((offerings.data ?? []).map((o) => o.programmeId))]
  const missingQueries = useQueries({
    queries: programmeIds.map((programmeId) => ({
      queryKey: ['farmers', 'missing', profile.data?.id, String(programmeId)],
      queryFn: () => getMissingPrerequisites(profile.data.id, programmeId),
      enabled: profile.data != null,
    })),
  })
  const missingFor = new Map(programmeIds.map((id, i) => [id, missingQueries[i]?.data]))
  const ready =
    offerings.data &&
    profile.data &&
    seasons.data &&
    contributions.data &&
    enrolments.data &&
    missingQueries.every((q) => q.data || q.isError) // a failed check shows as unknown, not as a block
  const loadError = offerings.error ?? profile.error ?? seasons.error ?? contributions.error ?? enrolments.error
  const ranked = ready
    ? rankOfferings(
        offerings.data.map((o) => ({
          offering: o,
          enrolled: enrolledIn.has(o.id),
          items: eligibilityChecklist({
            offering: o,
            season: seasonById.get(o.seasonId),
            profile: profile.data,
            contribution: contributionFor(o.seasonId),
            missing: missingFor.get(o.programmeId),
          }),
        })),
      )
    : []

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
        <SeasonFilter value={seasonId} onChange={(v) => setSearchParams(v ? { seasonId: v } : {}, { replace: true })} />
      </div>

      {loadError ? (
        <Alert tone="error">{toApiError(loadError).message}</Alert>
      ) : offerings.data?.length === 0 ? (
        <p className="rounded-lg border border-line bg-white px-4 py-8 text-center text-muted">
          No offerings in your district {seasonId ? 'for this season' : 'yet'}.
        </p>
      ) : !ready ? (
        <div className="py-12 text-center text-brand-700">
          <Spinner label="Checking the offerings for you" className="size-8" />
        </div>
      ) : (
        <>
          <p className="mb-3 text-sm text-muted">
            Ordered by how many requirements you meet: those you can enrol in come first.
          </p>
          <div className="grid gap-4 lg:grid-cols-2">
            {ranked.map((row) => (
              <OfferingCard
                key={row.offering.id}
                offering={row.offering}
                items={row.items}
                met={row.met}
                enrolled={row.enrolled}
                onEnrol={askEnrol}
              />
            ))}
          </div>
        </>
      )}

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
