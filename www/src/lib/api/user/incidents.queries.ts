import { keepPreviousData, queryOptions } from "@tanstack/react-query"

import { getIncidentStatus, getServerIncidents } from "@/lib/api/user/incidents"
import { userServerQueryKey } from "@/lib/api/user/servers.queries"
import type { PageSearchParams } from "@/lib/schemas/pagination"

const OPEN_INCIDENT_POLL_MS = 30_000

export function serverIncidentsQueryKey(
  serverId: number,
  pagination: PageSearchParams
) {
  return [...userServerQueryKey(serverId), "incidents", pagination] as const
}

export function serverOpenIncidentQueryKey(serverId: number) {
  return [...userServerQueryKey(serverId), "incidents", "open"] as const
}

export function serverIncidentsQueryOptions(
  serverId: number,
  pagination: PageSearchParams
) {
  return queryOptions({
    queryKey: serverIncidentsQueryKey(serverId, pagination),
    queryFn: () =>
      getServerIncidents(serverId, pagination.page, pagination.count),
    placeholderData: keepPreviousData,
    refetchInterval: (query) =>
      query.state.data?.items.some(
        (incident) => getIncidentStatus(incident) === "ongoing"
      )
        ? OPEN_INCIDENT_POLL_MS
        : false,
  })
}

export function serverOpenIncidentQueryOptions(serverId: number) {
  return queryOptions({
    queryKey: serverOpenIncidentQueryKey(serverId),
    queryFn: async () => {
      const page = await getServerIncidents(serverId, 1, 1)
      return (
        page.items.find((item) => getIncidentStatus(item) === "ongoing") ?? null
      )
    },
    refetchInterval: OPEN_INCIDENT_POLL_MS,
  })
}
