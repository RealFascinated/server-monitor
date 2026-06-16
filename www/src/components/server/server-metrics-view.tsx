import { useMemo } from "react"

import { Callout } from "@/components/callout"
import { MetricsView } from "@/components/metrics/metrics-view"
import {
  ServerOverviewStats,
  serverOverviewHasData,
} from "@/components/server/server-overview-stats"
import { useUserServer } from "@/hooks/use-user-server"
import type { ServerMetricsResponse } from "@/lib/api/user/metrics"
import type { MetricsDataWindow } from "@/lib/metrics/chart-zoom"
import { formatMetricTimeWindowDescription } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"
import { buildServerMetricSections } from "@/lib/metrics/sections/server/build"
import { buildMetricsTimeGrid } from "@/lib/metrics/timestamps"

type ServerMetricsViewProps = {
  metrics: ServerMetricsResponse
  timeWindow: MetricTimeWindow
  dataWindow: MetricsDataWindow
  onZoomToRange: (from: number, to: number) => void
  zoomDisabled?: boolean
}

function ServerMetricsView({
  metrics,
  timeWindow,
  dataWindow,
  onZoomToRange,
  zoomDisabled = false,
}: ServerMetricsViewProps) {
  const { data: server } = useUserServer(metrics.id)
  const timeGrid = useMemo(() => buildMetricsTimeGrid(metrics), [metrics])
  const sections = useMemo(
    () => buildServerMetricSections(metrics, timeGrid),
    [metrics, timeGrid]
  )

  const hasChartData = sections.length > 0
  const showRangeCallout = !hasChartData && server?.status !== "PENDING"

  if (sections.length === 0 && !showRangeCallout) {
    return null
  }

  const rangeLabel = formatMetricTimeWindowDescription(timeWindow)
  const hasCurrentMetrics = serverOverviewHasData(server)

  return (
    <div className="flex flex-col gap-6">
      {showRangeCallout ? (
        <Callout
          type={hasCurrentMetrics ? "info" : "warning"}
          title={
            hasCurrentMetrics
              ? "Not enough history yet"
              : "No metrics in this time range"
          }
        >
          <p>
            {hasCurrentMetrics
              ? `Metrics are being collected, but this server hasn't been monitored long enough to fill the ${rangeLabel}. Try a shorter range.`
              : `No chart data is available for the ${rangeLabel}. Try a shorter or more recent time range.`}
          </p>
        </Callout>
      ) : null}
      {serverOverviewHasData(server) ? (
        <ServerOverviewStats serverId={metrics.id} />
      ) : null}
      {sections.length > 0 ? (
        <MetricsView
          sections={sections}
          dataWindow={dataWindow}
          onZoomToRange={onZoomToRange}
          zoomDisabled={zoomDisabled}
        />
      ) : null}
    </div>
  )
}

export { ServerMetricsView }
