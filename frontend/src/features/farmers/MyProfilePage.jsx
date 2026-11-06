import { useQuery } from '@tanstack/react-query'
import { toApiError } from '../../api/errors'
import { getMyProfile } from '../../api/farmers'
import { PageHeader } from '../../components/layout/PageHeader'
import { Alert } from '../../components/ui/Alert'
import { Spinner } from '../../components/ui/Spinner'
import { FarmerDetailsCard } from './FarmerDetailsCard'
import { TrainingRecordsTable } from './TrainingRecordsTable'

/**
 * /my/profile: the logged-in farmer's own record and completed trainings (URD FR-FAR-10).
 * GET /api/me/profile always answers with the caller's own farmer, so no id is ever taken from the address.
 */
export function MyProfilePage() {
  const profile = useQuery({ queryKey: ['me', 'profile'], queryFn: getMyProfile })

  return (
    <>
      <PageHeader title="My profile" description="Your membership record with the Rwanda Cooperative Union." />
      {profile.isPending ? (
        <div className="py-12 text-center text-brand-700">
          <Spinner label="Loading your profile" className="size-8" />
        </div>
      ) : profile.error ? (
        <Alert tone="error">{toApiError(profile.error).message}</Alert>
      ) : (
        <div className="space-y-6">
          <FarmerDetailsCard farmer={profile.data} />
          <section className="space-y-4">
            <h2 className="text-lg font-semibold">My completed trainings</h2>
            <TrainingRecordsTable farmerId={profile.data.id} />
          </section>
          <p className="text-sm text-muted">
            If any of these details are wrong, ask the Union Registrar to correct them.
          </p>
        </div>
      )}
    </>
  )
}
