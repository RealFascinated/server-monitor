import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import {
  createAlertDestination,
  deleteAlertDestination,
  testAlertDestination,
  updateAlertDestination,
} from "@/lib/api/user/alerts"
import type { AlertDestinationUpdateRequest } from "@/lib/api/user/alerts"
import {
  alertDestinationsQueryKey,
  alertDestinationsQueryOptions,
  alertRulesQueryKey,
} from "@/lib/api/user/alerts.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

export function useAlertDestinations() {
  return useQuery(alertDestinationsQueryOptions())
}

/** Destinations change how many rules point at them, and the first one is attached to unassigned rules. */
function useInvalidateDestinations() {
  const queryClient = useQueryClient()

  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: alertDestinationsQueryKey }),
      queryClient.invalidateQueries({ queryKey: alertRulesQueryKey }),
    ])
}

export function useCreateAlertDestination() {
  const invalidate = useInvalidateDestinations()

  return useMutation({
    mutationFn: createAlertDestination,
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Destination added")
    },
    onError: (error) =>
      toastMutationError(
        "Could not add destination",
        error,
        "Failed to add destination"
      ),
  })
}

export function useUpdateAlertDestination() {
  const invalidate = useInvalidateDestinations()

  return useMutation({
    mutationFn: (variables: {
      id: number
      request: AlertDestinationUpdateRequest
    }) => updateAlertDestination(variables.id, variables.request),
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Destination updated")
    },
    onError: (error) =>
      toastMutationError(
        "Could not update destination",
        error,
        "Failed to update destination"
      ),
  })
}

export function useDeleteAlertDestination() {
  const invalidate = useInvalidateDestinations()

  return useMutation({
    mutationFn: (variables: { id: number; force: boolean }) =>
      deleteAlertDestination(variables.id, variables.force),
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Destination deleted")
    },
    onError: (error) =>
      toastMutationError(
        "Could not delete destination",
        error,
        "Failed to delete destination"
      ),
  })
}

export function useTestAlertDestination() {
  const invalidate = useInvalidateDestinations()

  return useMutation({
    mutationFn: testAlertDestination,
    onSuccess: () => toastSuccess("Test message sent"),
    onError: (error) =>
      toastMutationError(
        "Test message failed",
        error,
        "The test message could not be delivered"
      ),
    onSettled: () => void invalidate(),
  })
}
