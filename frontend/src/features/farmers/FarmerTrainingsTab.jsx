import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { recordFarmerTraining } from '../../api/farmers'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { useTrainingOptions } from '../reference/useReferenceOptions'
import { RecordTrainingDialog } from './RecordTrainingDialog'
import { TrainingRecordsTable } from './TrainingRecordsTable'
import { useFarmer } from './useFarmer'

/** Trainings tab: completed trainings; Record training for the Registrar (URD FR-FAR-06, FR-FAR-07). */
export function FarmerTrainingsTab() {
  const farmer = useFarmer()
  const { hasPermission } = useAuth()
  const canRecord = hasPermission('TRAINING_RECORD_MANAGE')
  const toast = useToast()
  const queryClient = useQueryClient()
  const trainings = useTrainingOptions()
  const [recording, setRecording] = useState(false)

  const record = useMutation({
    mutationFn: ({ trainingId, completedOn }) => recordFarmerTraining(farmer.id, trainingId, completedOn),
    onSuccess: async (saved) => {
      // Also refreshes the missing-prerequisites answers, which depend on these records.
      await queryClient.invalidateQueries({ queryKey: ['farmers', 'trainings', farmer.id] })
      await queryClient.invalidateQueries({ queryKey: ['farmers', 'missing', farmer.id] })
      toast.success(`${saved.trainingName} recorded`)
    },
  })

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Completed trainings</h2>
        {canRecord && <Button onClick={() => setRecording(true)}>Record training</Button>}
      </div>
      <TrainingRecordsTable farmerId={farmer.id} />
      {recording && (
        <RecordTrainingDialog
          trainings={trainings.data?.content ?? []}
          onSave={(trainingId, completedOn) => record.mutateAsync({ trainingId, completedOn })}
          onClose={() => setRecording(false)}
        />
      )}
    </section>
  )
}
