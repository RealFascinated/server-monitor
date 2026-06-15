import { apiFetch, ApiClientError } from "@/lib/auth/api"
import { emptyPage, type Page } from "@/lib/api/pagination"

export type IncidentResponse = {
  id: number
  serverId: number
  startedAt: string
  resolvedAt?: string | null
}

export type IncidentStatus = "ongoing" | "resolved"

export function getIncidentStatus(incident: IncidentResponse): IncidentStatus {
  return incident.resolvedAt == null ? "ongoing" : "resolved"
}

export function getIncidentDurationMs(
  incident: IncidentResponse,
  now = Date.now()
): number {
  const end = incident.resolvedAt ?? new Date(now).toISOString()
  return new Date(end).getTime() - new Date(incident.startedAt).getTime()
}

export function getServerIncidents(
  serverId: number,
  page: number,
  count: number
): Promise<Page<IncidentResponse>> {
  return apiFetch<Page<IncidentResponse>>(
    `/v1/servers/${serverId}/incidents?page=${page}&count=${count}`
  ).catch((error) => {
    if (
      page === 1 &&
      error instanceof ApiClientError &&
      error.status === 400
    ) {
      return emptyPage<IncidentResponse>(count)
    }

    throw error
  })
}
