import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { toApiError } from '../../api/errors'
import { getOffering, updateOffering } from '../../api/offerings'
import { useAuth } from '../../auth/useAuth'
import { PageHeader } from '../../components/layout/PageHeader'
import { useToast } from '../../components/toast/useToast'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Spinner } from '../../components/ui/Spinner'
import { ForbiddenPage } from '../errors/ForbiddenPage'
import { EditOfferingDialog } from './EditOfferingDialog'
import { PlacesCell } from './OfferingsTable'

/**
 * /offerings/:id: one offering (URD FR-OFF-04, FR-OFF-05). This branch builds the Overview; the Enrolments,
 * Inspections and Results tabs arrive with the enrolment and the inspections-and-results modules.
 * The backend answers 403 for an offering outside a district user's own district.
 */
export function OfferingPage() {
  const id = Number(useParams().id)
  const { hasPermission } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const offering = useQuery({ queryKey: ['offerings', 'detail', id], queryFn: () => getOffering(id) })

  const save = useMutation({
    mutationFn: (values) => updateOffering(id, values),
    onSuccess: async (o) => {
      queryClient.setQueryData(['offerings', 'detail', id], o)
      await queryClient.invalidateQueries({ queryKey: ['offerings', 'list'] })
      await queryClient.invalidateQueries({ queryKey: ['offerings', 'mine'] })
      toast.success('Offering saved')
    },
  })

  if (offering.isPending) {
    return (
      <div className="py-12 text-center text-brand-700">
        <Spinner label="Loading the offering" className="size-8" />
      </div>
    )
  }
  if (offering.error) {
    const error = toApiError(offering.error)
    return error.status === 403 ? <ForbiddenPage /> : <Alert tone="error">{error.message}</Alert>
  }

  const o = offering.data
  const rows = [
    { label: 'Crop programme', value: o.programmeName },
    {
      label: 'Season',
      value: (
        <Link to={`/seasons/${o.seasonId}`} className="font-medium text-brand-700 hover:underline">
          {o.seasonName}
        </Link>
      ),
    },
    { label: 'District', value: o.districtName },
    { label: 'Field Officer', value: o.fieldOfficerUsername },
    { label: 'Hectares per participant', value: `${o.hectaresPerParticipant} ha` },
    { label: 'Enrolled / maximum', value: <PlacesCell offering={o} /> },
    { label: 'Places left', value: o.remainingPlaces },
  ]

  return (
    <>
      <Link to="/offerings" className="mb-3 inline-block text-sm font-semibold text-brand-600 hover:underline">
        ← Offerings
      </Link>
      <PageHeader
        title={`${o.programmeName}, ${o.seasonName}`}
        description={`${o.districtName} district`}
        actions={
          hasPermission('OFFERING_MANAGE') && (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
          )
        }
      />
      <section className="rounded-lg border border-line bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold">Overview</h2>
        <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-3">
          {rows.map(({ label, value }) => (
            <div key={label}>
              <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</dt>
              <dd className="mt-1 text-ink">{value}</dd>
            </div>
          ))}
        </dl>
      </section>
      {editing && <EditOfferingDialog offering={o} onSave={save.mutateAsync} onClose={() => setEditing(false)} />}
    </>
  )
}
