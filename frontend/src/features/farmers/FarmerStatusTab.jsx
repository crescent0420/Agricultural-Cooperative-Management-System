import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { changeFarmerStatus } from '../../api/farmers'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { ChangeStatusDialog } from './ChangeStatusDialog'
import { NEXT_STATUSES, STATUS_LABELS, STATUS_MEANINGS } from './farmerStatus'
import { StatusBadge } from './StatusBadge'
import { useFarmer } from './useFarmer'

/** Status tab: what the current status means, and Change status for the Registrar (URD FR-FAR-05). */
export function FarmerStatusTab() {
  const farmer = useFarmer()
  const { hasPermission } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [changing, setChanging] = useState(false)
  const next = NEXT_STATUSES[farmer.status] ?? []
  const canChange = hasPermission('FARMER_STATUS_CHANGE') && next.length > 0

  const save = useMutation({
    mutationFn: (status) => changeFarmerStatus(farmer.id, status),
    onSuccess: async (saved) => {
      queryClient.setQueryData(['farmers', 'detail', farmer.id], saved)
      await queryClient.invalidateQueries({ queryKey: ['farmers', 'search'] })
      toast.success(`Status changed to ${STATUS_LABELS[saved.status]}`)
    },
  })

  return (
    <section className="space-y-4 rounded-lg border border-line bg-white p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="flex items-center gap-3 text-lg font-semibold">
          Status <StatusBadge status={farmer.status} />
        </h2>
        {canChange && <Button onClick={() => setChanging(true)}>Change status</Button>}
      </div>
      <p className="text-sm">{STATUS_MEANINGS[farmer.status]}</p>
      {next.length > 0 && (
        <p className="text-sm text-muted">
          Can be changed to: {next.map((s) => STATUS_LABELS[s]).join(', ')}. Only the Union Registrar changes a
          farmer&apos;s status.
        </p>
      )}
      {changing && <ChangeStatusDialog farmer={farmer} onSave={save.mutateAsync} onClose={() => setChanging(false)} />}
    </section>
  )
}
