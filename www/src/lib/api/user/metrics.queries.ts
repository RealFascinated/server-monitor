import { keepPreviousData, queryOptions } from "@tanstack/react-query"

import { getUserServerMetrics } from "@/lib/api/user/metrics"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"
import { metricTimeWindowQueryKey } from "@/lib/metrics/time-window"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { getMetricRefreshIntervalMs } from "@/lib/metrics/refresh-interval"

export const userServerMetricsQueryKey = {
  all: ["user", "servers"] as const,
  server: (serverId: number) =>
    ["user", "servers", serverId, "metrics"] as const,
  detail: (serverId: number, window: MetricTimeWindow) =>
    [
      "user",
      "servers",
      serverId,
      "metrics",
      metricTimeWindowQueryKey(window),
    ] as const,
}

export function userServerMetricsQueryOptions(
  serverId: number,
  window: MetricTimeWindow,
  refreshInterval: MetricRefreshInterval = "10s"
) {
  return queryOptions({
    queryKey: userServerMetricsQueryKey.detail(serverId, window),
    queryFn: () => getUserServerMetrics(serverId, window),
    placeholderData: keepPreviousData,
    refetchInterval: getMetricRefreshIntervalMs(refreshInterval),
  })
}
