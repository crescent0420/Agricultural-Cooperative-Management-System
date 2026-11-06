import { useMutation, useQueryClient } from '@tanstack/react-query'
import { cancelEnrolment } from '../../api/enrolments'
import { useAuth } from '../../auth/useAuth'
import { useToast } from '../../components/toast/useToast'

/**
 * Cancelling, shared by every screen that offers it. E1: EnrolmentService.cancel requires a reason from
 * everyone except the farmer, so the rule follows the backend's own test (the role), not a permission.
 */
export function useCancelEnrolment() {
  const { user } = useAuth()
  const toast = useToast()
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: ({ id, reason }) => cancelEnrolment(id, reason),
    onSuccess: async (e) => {
      // Counts, places and lists all change.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['enrolments'] }),
        queryClient.invalidateQueries({ queryKey: ['offerings'] }),
      ])
      toast.success(`Enrolment in ${e.programmeName} (${e.seasonName}) cancelled`)
    },
  })
  return { cancel: mutation.mutateAsync, reasonRequired: user?.role !== 'FARMER' }
}
