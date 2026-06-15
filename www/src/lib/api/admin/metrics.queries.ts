import { queryOptions } from "@tanstack/react-query"

import { getAdminMetrics } from "@/lib/api/admin/metrics"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"
import { metricTimeWindowQueryKey } from "@/lib/metrics/time-window"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { getMetricRefreshIntervalMs } from "@/lib/metrics/refresh-interval"

export function adminMetricsQueryOptions(
  window: MetricTimeWindow,
  refreshInterval: MetricRefreshInterval = "10s"
) {
  return queryOptions({
    queryKey: ["admin", "metrics", metricTimeWindowQueryKey(window)],
    queryFn: () => getAdminMetrics(window),
    refetchInterval: getMetricRefreshIntervalMs(refreshInterval),
  })
}
