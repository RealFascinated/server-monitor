import { useMutation, useQueryClient } from "@tanstack/react-query"

import { silenceAlertEvent, unsilenceAlertEvent } from "@/lib/api/user/alerts"
import { alertsQueryKey } from "@/lib/api/user/alerts.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

/** Event lists, rule and group counts and the sidebar badge all change when an alert is (un)silenced. */
function useInvalidateAlerts() {
  const queryClient = useQueryClient()

  return () => queryClient.invalidateQueries({ queryKey: alertsQueryKey })
}

export function useSilenceAlertEvent() {
  const invalidate = useInvalidateAlerts()

  return useMutation({
    mutationFn: silenceAlertEvent,
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Alert silenced until it gets worse")
    },
    onError: (error) =>
      toastMutationError(
        "Could not silence alert",
        error,
        "Failed to silence alert"
      ),
  })
}

export function useUnsilenceAlertEvent() {
  const invalidate = useInvalidateAlerts()

  return useMutation({
    mutationFn: unsilenceAlertEvent,
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Alert unsilenced")
    },
    onError: (error) =>
      toastMutationError(
        "Could not unsilence alert",
        error,
        "Failed to unsilence alert"
      ),
  })
}
