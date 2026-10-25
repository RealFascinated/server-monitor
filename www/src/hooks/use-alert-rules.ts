import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import {
  createAlertRule,
  deleteAlertRule,
  setAlertRuleEnabled,
  updateAlertRule,
} from "@/lib/api/user/alerts"
import type { AlertRuleRequest } from "@/lib/api/user/alerts"
import {
  alertGroupsQueryKey,
  alertRuleQueryOptions,
  alertRulesQueryKey,
  alertRulesQueryOptions,
} from "@/lib/api/user/alerts.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

export function useAlertRules(groupId?: number) {
  return useQuery(alertRulesQueryOptions(groupId))
}

export function useAlertRule(id: number) {
  return useQuery(alertRuleQueryOptions(id))
}

/** Rule counts and firing counts live on the group list too. */
function useInvalidateRules() {
  const queryClient = useQueryClient()

  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: alertRulesQueryKey }),
      queryClient.invalidateQueries({ queryKey: alertGroupsQueryKey }),
    ])
}

export function useCreateAlertRule() {
  const invalidate = useInvalidateRules()

  return useMutation({
    mutationFn: (request: AlertRuleRequest) => createAlertRule(request),
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Alert created")
    },
    onError: (error) =>
      toastMutationError(
        "Could not create alert",
        error,
        "Failed to create alert"
      ),
  })
}

export function useUpdateAlertRule() {
  const invalidate = useInvalidateRules()

  return useMutation({
    mutationFn: (variables: { id: number; request: AlertRuleRequest }) =>
      updateAlertRule(variables.id, variables.request),
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Alert updated")
    },
    onError: (error) =>
      toastMutationError(
        "Could not update alert",
        error,
        "Failed to update alert"
      ),
  })
}

export function useSetAlertRuleEnabled() {
  const invalidate = useInvalidateRules()

  return useMutation({
    mutationFn: (variables: { id: number; enabled: boolean }) =>
      setAlertRuleEnabled(variables.id, variables.enabled),
    onSuccess: () => invalidate(),
    onError: (error) =>
      toastMutationError(
        "Could not update alert",
        error,
        "Failed to update alert"
      ),
  })
}

export function useDeleteAlertRule() {
  const invalidate = useInvalidateRules()

  return useMutation({
    mutationFn: deleteAlertRule,
    onSuccess: async () => {
      await invalidate()
      toastSuccess("Alert deleted")
    },
    onError: (error) =>
      toastMutationError(
        "Could not delete alert",
        error,
        "Failed to delete alert"
      ),
  })
}
