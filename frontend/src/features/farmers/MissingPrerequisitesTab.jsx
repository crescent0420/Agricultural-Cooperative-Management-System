import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getMissingPrerequisites } from '../../api/farmers'
import { Alert } from '../../components/ui/Alert'
import { Select } from '../../components/ui/Select'
import { Spinner } from '../../components/ui/Spinner'
import { useProgrammeOptions } from '../reference/useReferenceOptions'
import { useFarmer } from './useFarmer'

/**
 * Missing prerequisites tab (URD FR-FAR-08): pick a crop programme of the farmer's district and see which of
 * its prerequisite trainings the farmer has not completed. The backend works out the answer.
 */
export function MissingPrerequisitesTab() {
  const farmer = useFarmer()
  const programmes = useProgrammeOptions(farmer.districtId)
  const [programmeId, setProgrammeId] = useState('')

  const missing = useQuery({
    queryKey: ['farmers', 'missing', farmer.id, programmeId],
    queryFn: () => getMissingPrerequisites(farmer.id, Number(programmeId)),
    enabled: programmeId !== '',
  })
  const programmeName = programmes.data?.content.find((p) => String(p.id) === programmeId)?.name

  return (
    <section className="space-y-4 rounded-lg border border-line bg-white p-6">
      <h2 className="text-lg font-semibold">Missing prerequisite trainings</h2>
      <Select
        label="Crop programme"
        className="max-w-sm"
        value={programmeId}
        hint={`Programmes of ${farmer.districtName} district`}
        onChange={(e) => setProgrammeId(e.target.value)}
      >
        <option value="">Choose a programme…</option>
        {programmes.data?.content.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name}
          </option>
        ))}
      </Select>

      {programmeId !== '' &&
        (missing.isPending ? (
          <Spinner label="Checking the trainings" />
        ) : missing.error ? (
          <Alert tone="error">{missing.error.message}</Alert>
        ) : missing.data.length === 0 ? (
          <Alert tone="success">
            None missing: {farmer.fullName} has completed every prerequisite training of {programmeName}.
          </Alert>
        ) : (
          <div>
            <p className="mb-2 text-sm">
              {farmer.fullName} still needs {missing.data.length === 1 ? 'this training' : 'these trainings'} to enrol
              in {programmeName}:
            </p>
            <ul aria-label="Missing trainings" className="list-disc space-y-1 pl-6 text-sm">
              {missing.data.map((t) => (
                <li key={t.id}>{t.name}</li>
              ))}
            </ul>
          </div>
        ))}
    </section>
  )
}
