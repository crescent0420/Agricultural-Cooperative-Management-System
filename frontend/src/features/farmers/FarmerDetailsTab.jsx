import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { updateFarmer } from '../../api/farmers'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'
import { Button } from '../../components/ui/Button'
import { EditFarmerDialog } from './EditFarmerDialog'
import { FarmerDetailsCard } from './FarmerDetailsCard'
import { useFarmer } from './useFarmer'

/** Details tab: the record, and Edit for the Registrar (URD FR-FAR-02, FR-FAR-04). */
export function FarmerDetailsTab() {
  const farmer = useFarmer()
  const { hasPermission } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)

  const save = useMutation({
    mutationFn: (changes) => updateFarmer(farmer.id, changes),
    onSuccess: async (saved) => {
      queryClient.setQueryData(['farmers', 'detail', farmer.id], saved)
      await queryClient.invalidateQueries({ queryKey: ['farmers', 'search'] })
      toast.success('Farmer details saved')
    },
  })

  return (
    <>
      <FarmerDetailsCard
        farmer={farmer}
        actions={
          hasPermission('FARMER_MANAGE') && (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              Edit
            </Button>
          )
        }
      />
      {editing && <EditFarmerDialog farmer={farmer} onSave={save.mutateAsync} onClose={() => setEditing(false)} />}
    </>
  )
}
