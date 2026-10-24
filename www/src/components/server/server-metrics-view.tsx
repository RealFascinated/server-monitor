import { useQuery } from "@tanstack/react-query"
import { useMemo } from "react"

import { Callout } from "@/components/callout"
import { MetricsView } from "@/components/metrics/metrics-view"
import {
  ServerOverviewStats,
  serverOverviewHasData,
} from "@/components/server/server-overview-stats"
import { useUserServer } from "@/hooks/use-user-server"
import { serverIncidentsQueryOptions } from "@/lib/api/user/incidents.queries"
import {
  incidentsToSpans,
  MetricsChartAnnotationsProvider,
} from "@/lib/metrics/chart-annotations"
import type { ServerMetricsResponse } from "@/lib/api/user/metrics"
import type { MetricsDataWindow } from "@/lib/metrics/chart-zoom"
import { formatMetricTimeWindowDescription } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"
import { buildServerMetricSections } from "@/lib/metrics/sections/server/build"
import { buildServerSparklines } from "@/lib/metrics/sparkline"
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
  const { data: incidentPage } = useQuery(
    serverIncidentsQueryOptions(metrics.id, { page: 1, count: 50 })
  )
  const incidentSpans = useMemo(
    () =>
      incidentsToSpans(incidentPage?.items ?? [], {
        from: metrics.from,
        to: metrics.to,
      }),
    [incidentPage, metrics.from, metrics.to]
  )
  const trends = useMemo(() => buildServerSparklines(metrics), [metrics])
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
        <ServerOverviewStats
          serverId={metrics.id}
          trends={trends}
          rangeLabel={rangeLabel}
        />
      ) : null}
      {sections.length > 0 ? (
        <MetricsChartAnnotationsProvider spans={incidentSpans}>
          {incidentSpans.length > 0 ? (
            <p className="flex items-center gap-2 text-xs text-muted-foreground">
              <span
                aria-hidden
                className="h-3.5 w-2.5 shrink-0 border-l-2 border-error bg-error/25"
              />
              Shaded areas on the charts mark periods when the agent stopped
              reporting.
            </p>
          ) : null}
          <MetricsView
            sections={sections}
            dataWindow={dataWindow}
            onZoomToRange={onZoomToRange}
            zoomDisabled={zoomDisabled}
          />
        </MetricsChartAnnotationsProvider>
      ) : null}
    </div>
  )
}

export { ServerMetricsView }
