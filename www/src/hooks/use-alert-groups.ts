import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import {
  createAlertGroup,
  deleteAlertGroup,
  updateAlertGroup,
} from "@/lib/api/user/alerts"
import type { AlertGroupUpdateRequest } from "@/lib/api/user/alerts"
import {
  alertGroupQueryOptions,
  alertGroupsQueryKey,
  alertGroupsQueryOptions,
  alertRulesQueryKey,
} from "@/lib/api/user/alerts.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

export function useAlertGroups() {
  return useQuery(alertGroupsQueryOptions())
}

export function useAlertGroup(id: number) {
  return useQuery(alertGroupQueryOptions(id))
}

/** Group changes alter which servers inherit which rules, and delete cascades to rules. */
function useInvalidateGroups() {
  const queryClient = useQueryClient()

  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: alertGroupsQueryKey }),
      queryClient.invalidateQueries({ queryKey: alertRulesQueryKey }),
    ])
}

export function useCreateAlertGroup() {
  const invalidate = useInvalidateGroups()

  return useMutation({
    mutationFn: createAlertGroup,
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Group created")
    },
    onError: (error) =>
      toastMutationError(
        "Could not create group",
        error,
        "Failed to create group"
      ),
  })
}

export function useUpdateAlertGroup() {
  const invalidate = useInvalidateGroups()

  return useMutation({
    mutationFn: (variables: { id: number; request: AlertGroupUpdateRequest }) =>
      updateAlertGroup(variables.id, variables.request),
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Group updated")
    },
    onError: (error) =>
      toastMutationError(
        "Could not update group",
        error,
        "Failed to update group"
      ),
  })
}

export function useDeleteAlertGroup() {
  const invalidate = useInvalidateGroups()

  return useMutation({
    mutationFn: deleteAlertGroup,
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Group deleted")
    },
    onError: (error) =>
      toastMutationError(
        "Could not delete group",
        error,
        "Failed to delete group"
      ),
  })
}
