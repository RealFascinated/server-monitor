import { keepPreviousData, queryOptions } from "@tanstack/react-query"

import {
  getAlertDestinations,
  getAlertEvents,
  getAlertFiringCount,
  getAlertGroup,
  getAlertGroups,
  getAlertMetrics,
  getAlertRule,
  getAlertRules,
  getAlertSeries,
  previewAlertRule,
} from "@/lib/api/user/alerts"
import type {
  AlertEventFilters,
  AlertPreviewRequest,
} from "@/lib/api/user/alerts"
import type { PageSearchParams } from "@/lib/schemas/pagination"

const FIRING_POLL_MS = 30_000

export const alertsQueryKey = ["user", "alerts"] as const

export const alertDestinationsQueryKey = [
  ...alertsQueryKey,
  "destinations",
] as const

export const alertRulesQueryKey = [...alertsQueryKey, "rules"] as const

export const alertGroupsQueryKey = [...alertsQueryKey, "groups"] as const

export function alertGroupsQueryOptions() {
  return queryOptions({
    queryKey: alertGroupsQueryKey,
    queryFn: getAlertGroups,
    refetchInterval: (query) =>
      query.state.data?.some((group) => group.firingCount > 0)
        ? FIRING_POLL_MS
        : false,
  })
}

export function alertGroupQueryOptions(id: number) {
  return queryOptions({
    queryKey: [...alertGroupsQueryKey, "detail", id] as const,
    queryFn: () => getAlertGroup(id),
  })
}

export function alertDestinationsQueryOptions() {
  return queryOptions({
    queryKey: alertDestinationsQueryKey,
    queryFn: getAlertDestinations,
  })
}

export function alertRulesQueryOptions(groupId?: number) {
  return queryOptions({
    queryKey: [...alertRulesQueryKey, groupId ?? "all"] as const,
    queryFn: () => getAlertRules(groupId),
    refetchInterval: (query) =>
      query.state.data?.some((rule) => rule.firingCount > 0)
        ? FIRING_POLL_MS
        : false,
  })
}

export function alertRuleQueryOptions(id: number) {
  return queryOptions({
    queryKey: [...alertRulesQueryKey, "detail", id] as const,
    queryFn: () => getAlertRule(id),
  })
}

export function alertEventsQueryOptions(
  pagination: PageSearchParams,
  filters: AlertEventFilters = {}
) {
  return queryOptions({
    queryKey: [...alertsQueryKey, "events", pagination, filters] as const,
    queryFn: () => getAlertEvents(pagination.page, pagination.count, filters),
    placeholderData: keepPreviousData,
    refetchInterval: (query) =>
      query.state.data?.items.some((event) => event.resolvedAt == null)
        ? FIRING_POLL_MS
        : false,
  })
}

export function alertFiringCountQueryOptions() {
  return queryOptions({
    queryKey: [...alertsQueryKey, "firing-count"] as const,
    queryFn: getAlertFiringCount,
    refetchInterval: FIRING_POLL_MS,
  })
}

export function alertMetricsQueryOptions() {
  return queryOptions({
    queryKey: [...alertsQueryKey, "metrics"] as const,
    queryFn: getAlertMetrics,
    staleTime: Number.POSITIVE_INFINITY,
  })
}

export function alertSeriesQueryOptions(serverId: number, metric: string) {
  return queryOptions({
    queryKey: [...alertsQueryKey, "series", serverId, metric] as const,
    queryFn: () => getAlertSeries(serverId, metric),
  })
}

export function alertPreviewQueryOptions(request: AlertPreviewRequest) {
  return queryOptions({
    queryKey: [...alertsQueryKey, "preview", request] as const,
    queryFn: () => previewAlertRule(request),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
  })
}
