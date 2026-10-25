import { apiFetch } from "@/lib/auth/api"
import type { Page } from "@/lib/api/pagination"

export type AlertUnit =
  | "PERCENT"
  | "BYTES"
  | "BYTES_PER_SECOND"
  | "MILLISECONDS"
  | "TEMPERATURE"
  | "WATTS"
  | "COUNT"
  | "STATE"
  | "NONE"

export type AlertOperator = "LESS_THAN" | "MORE_THAN"

export type AlertSeverity = "WARNING" | "CRITICAL"

export type AlertDestinationType = "DISCORD_WEBHOOK"

export type AlertDestination = {
  id: number
  name: string
  type: AlertDestinationType
  maskedUrl: string
  ruleCount: number
}

export type AlertDestinationRequest = {
  name: string
  type: AlertDestinationType
  config: string
}

export type AlertDestinationUpdateRequest = {
  name?: string
  config?: string
}

export type AlertGroupServer = {
  id: number
  name: string
}

export type AlertGroup = {
  id: number
  name: string
  autoAddServers: boolean
  enabled: boolean
  /** The group every account starts with; it can't be deleted. */
  defaultGroup: boolean
  servers: AlertGroupServer[]
  ruleCount: number
  firingCount: number
  createdAt: string
}

export type AlertGroupRequest = {
  name: string
  autoAddServers?: boolean
  serverIds?: number[]
}

export type AlertGroupUpdateRequest = {
  name?: string
  autoAddServers?: boolean
  enabled?: boolean
  /** Replaces the whole server list. */
  serverIds?: number[]
}

export type AlertRule = {
  id: number
  groupId: number
  groupName: string
  destinationId?: number | null
  destinationName?: string | null
  name?: string | null
  metric: string
  metricLabel: string
  unit: AlertUnit
  labelFilter: Record<string, string>
  operator: AlertOperator
  threshold: number
  recoveryThreshold?: number | null
  windowSeconds: number
  severity: AlertSeverity
  repeatIntervalSeconds?: number | null
  enabled: boolean
  createdAt: string
  firingCount: number
  silencedCount: number
  lastFiredAt?: string | null
}

export type AlertRuleRequest = {
  groupId: number
  destinationId?: number | null
  name?: string | null
  metric: string
  labelFilter?: Record<string, string>
  operator: AlertOperator
  threshold: number
  recoveryThreshold?: number | null
  windowSeconds?: number | null
  severity: AlertSeverity
  repeatIntervalSeconds?: number | null
  enabled?: boolean
}

export type AlertEvent = {
  id: number
  ruleId: number
  ruleName?: string | null
  metric: string
  metricLabel: string
  unit: AlertUnit
  operator: AlertOperator
  threshold: number
  serverId: number
  serverName?: string | null
  labels: Record<string, string>
  startedAt: string
  resolvedAt?: string | null
  resolveReason?: string | null
  peakValue: number
  lastNotifiedAt?: string | null
  notifyError?: string | null
  silencedAt?: string | null
  silencedValue?: number | null
}

export type AlertEventState = "firing" | "resolved"

export type AlertMetric = {
  metric: string
  label: string
  unit: AlertUnit
  defaultOperator: AlertOperator
  defaultThreshold: number
  group: string
  labelKeys: string[]
}

export type AlertSeriesResponse = {
  series: Record<string, string>[]
}

export type AlertPreviewRequest = {
  serverId: number
  metric: string
  labelFilter?: Record<string, string>
  operator: AlertOperator
  threshold: number
  recoveryThreshold?: number | null
  windowSeconds?: number | null
  rangeHours: number
}

export type AlertPreviewInterval = {
  from: number
  to: number
  ongoing: boolean
  peak: number
}

export type AlertPreviewSeries = {
  labels: Record<string, string>
  timestamps: number[]
  values: (number | null)[]
  firing: AlertPreviewInterval[]
}

export type AlertPreview = {
  unit: AlertUnit
  from: number
  to: number
  stepSeconds: number
  series: AlertPreviewSeries[]
  firingCount: number
  truncated: boolean
}

export type AlertEventFilters = {
  ruleId?: number
  groupId?: number
  serverId?: number
  state?: AlertEventState
}

export function getAlertDestinations(): Promise<AlertDestination[]> {
  return apiFetch<AlertDestination[]>("/v1/alerts/destinations")
}

export function createAlertDestination(
  request: AlertDestinationRequest
): Promise<AlertDestination> {
  return apiFetch<AlertDestination>("/v1/alerts/destinations", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function updateAlertDestination(
  id: number,
  request: AlertDestinationUpdateRequest
): Promise<AlertDestination> {
  return apiFetch<AlertDestination>(`/v1/alerts/destinations/${id}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  })
}

export function deleteAlertDestination(
  id: number,
  force: boolean
): Promise<void> {
  return apiFetch<void>(`/v1/alerts/destinations/${id}?force=${force}`, {
    method: "DELETE",
  })
}

export function testAlertDestination(id: number): Promise<void> {
  return apiFetch<void>(`/v1/alerts/destinations/${id}/test`, {
    method: "POST",
  })
}

export function getAlertGroups(): Promise<AlertGroup[]> {
  return apiFetch<AlertGroup[]>("/v1/alerts/groups")
}

export function getAlertGroup(id: number): Promise<AlertGroup> {
  return apiFetch<AlertGroup>(`/v1/alerts/groups/${id}`)
}

export function createAlertGroup(
  request: AlertGroupRequest
): Promise<AlertGroup> {
  return apiFetch<AlertGroup>("/v1/alerts/groups", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function updateAlertGroup(
  id: number,
  request: AlertGroupUpdateRequest
): Promise<AlertGroup> {
  return apiFetch<AlertGroup>(`/v1/alerts/groups/${id}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  })
}

export function deleteAlertGroup(id: number): Promise<void> {
  return apiFetch<void>(`/v1/alerts/groups/${id}`, { method: "DELETE" })
}

export function getAlertRules(groupId?: number): Promise<AlertRule[]> {
  const query = groupId != null ? `?groupId=${groupId}` : ""
  return apiFetch<AlertRule[]>(`/v1/alerts/rules${query}`)
}

export function getAlertRule(id: number): Promise<AlertRule> {
  return apiFetch<AlertRule>(`/v1/alerts/rules/${id}`)
}

export function createAlertRule(request: AlertRuleRequest): Promise<AlertRule> {
  return apiFetch<AlertRule>("/v1/alerts/rules", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function updateAlertRule(
  id: number,
  request: AlertRuleRequest
): Promise<AlertRule> {
  return apiFetch<AlertRule>(`/v1/alerts/rules/${id}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  })
}

export function setAlertRuleEnabled(
  id: number,
  enabled: boolean
): Promise<AlertRule> {
  return apiFetch<AlertRule>(`/v1/alerts/rules/${id}/enabled`, {
    method: "POST",
    body: JSON.stringify({ enabled }),
  })
}

export function deleteAlertRule(id: number): Promise<void> {
  return apiFetch<void>(`/v1/alerts/rules/${id}`, { method: "DELETE" })
}

export function getAlertEvents(
  page: number,
  count: number,
  filters: AlertEventFilters = {}
): Promise<Page<AlertEvent>> {
  const params = new URLSearchParams({
    page: String(page),
    count: String(count),
  })
  if (filters.ruleId != null) params.set("ruleId", String(filters.ruleId))
  if (filters.groupId != null) params.set("groupId", String(filters.groupId))
  if (filters.serverId != null) params.set("serverId", String(filters.serverId))
  if (filters.state) params.set("state", filters.state)

  return apiFetch<Page<AlertEvent>>(`/v1/alerts/events?${params}`)
}

/** Mutes a firing alert until its value gets worse than it is now. */
export function silenceAlertEvent(id: number): Promise<void> {
  return apiFetch<void>(`/v1/alerts/events/${id}/silence`, { method: "POST" })
}

export function unsilenceAlertEvent(id: number): Promise<void> {
  return apiFetch<void>(`/v1/alerts/events/${id}/silence`, {
    method: "DELETE",
  })
}

export function getAlertFiringCount(): Promise<number> {
  return apiFetch<number>("/v1/alerts/firing-count")
}

export function getAlertMetrics(): Promise<AlertMetric[]> {
  return apiFetch<AlertMetric[]>("/v1/alerts/metrics")
}

export function getAlertSeries(
  serverId: number,
  metric: string
): Promise<AlertSeriesResponse> {
  const params = new URLSearchParams({
    serverId: String(serverId),
    metric,
  })
  return apiFetch<AlertSeriesResponse>(`/v1/alerts/series?${params}`)
}

export function previewAlertRule(
  request: AlertPreviewRequest
): Promise<AlertPreview> {
  return apiFetch<AlertPreview>("/v1/alerts/preview", {
    method: "POST",
    body: JSON.stringify(request),
  })
}
