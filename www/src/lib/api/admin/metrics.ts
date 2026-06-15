import type { MetricValues } from "@/lib/api/user/metrics"
import { apiFetch } from "@/lib/auth/api"
import { metricTimeWindowToEpochWindow } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"

export type { MetricTimeRange } from "@/lib/metrics/range"
export type { MetricValues }

export type FleetOsMetrics = {
  os: string
  serversByOs: MetricValues
}

export type FleetVersionMetrics = {
  version: string
  serversByAgentVersion: MetricValues
}

export type OverviewMetrics = {
  activeSessions?: MetricValues
  databaseSizeBytes?: MetricValues
  users?: MetricValues
  usersNew24h?: MetricValues
}

export type FleetMetrics = {
  serversNew24h?: MetricValues
  serversOffline?: MetricValues
  serversOnline?: MetricValues
  serversPending?: MetricValues
  serversTotal?: MetricValues
  byOs?: FleetOsMetrics[]
  byAgentVersion?: FleetVersionMetrics[]
}

export type HttpMetricsEntry = {
  method: string
  path: string
  status: string
  httpRequestsTotal: MetricValues
}

export type HttpMetrics = Record<string, HttpMetricsEntry>

export type IngestMetrics = {
  ingestAuthFailuresTotal?: MetricValues
  ingestDurationSeconds?: MetricValues
  ingestPayloadBytes?: MetricValues
  ingestsTotal?: MetricValues
}

export type JvmMetrics = {
  jvmHeapMaxBytes?: MetricValues
  jvmHeapUsedBytes?: MetricValues
  jvmNonheapUsedBytes?: MetricValues
  jvmProcessCpuLoad?: MetricValues
  jvmProcessRssBytes?: MetricValues
  jvmThreadCount?: MetricValues
  jvmUptimeSeconds?: MetricValues
}

export type VmMetrics = {
  vmQueriesTotal?: MetricValues
  vmQueryDurationSeconds?: MetricValues
  vmQueryErrorsTotal?: MetricValues
  vmWriteDurationSeconds?: MetricValues
  vmWriteErrorsTotal?: MetricValues
  vmWritesTotal?: MetricValues
}

export type AdminMetricsResponse = {
  from: number
  to: number
  step: number | null
  timestamps: number[] | null
  overview?: OverviewMetrics | null
  fleet?: FleetMetrics | null
  http?: HttpMetrics | null
  ingest?: IngestMetrics | null
  jvm?: JvmMetrics | null
  vm?: VmMetrics | null
}

export function getAdminMetrics(
  window: MetricTimeWindow
): Promise<AdminMetricsResponse> {
  const { from, to } = metricTimeWindowToEpochWindow(window)
  const params = new URLSearchParams({
    from: String(from),
    to: String(to),
  })
  return apiFetch<AdminMetricsResponse>(`/v1/admin/metrics?${params}`)
}
