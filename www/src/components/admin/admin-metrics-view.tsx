import { useMemo } from "react"

import { Callout } from "@/components/callout"
import {
  AdminOverviewStats,
  adminOverviewHasData,
} from "@/components/admin/admin-overview-stats"
import { MetricsView } from "@/components/metrics/metrics-view"
import type { AdminMetricsResponse } from "@/lib/api/admin/metrics"
import { chartsHaveData } from "@/lib/metrics/chart-config"
import type { MetricsDataWindow } from "@/lib/metrics/chart-zoom"
import { buildAdminMetricSections } from "@/lib/metrics/sections/admin/build"
import { overviewCharts } from "@/lib/metrics/sections/admin/charts"
import { formatMetricTimeWindowDescription } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"
import { buildMetricsTimeGrid } from "@/lib/metrics/timestamps"

type AdminMetricsViewProps = {
  metrics: AdminMetricsResponse
  timeWindow: MetricTimeWindow
  dataWindow: MetricsDataWindow
  onZoomToRange: (from: number, to: number) => void
  zoomDisabled?: boolean
}

function AdminMetricsView({
  metrics,
  timeWindow,
  dataWindow,
  onZoomToRange,
  zoomDisabled = false,
}: AdminMetricsViewProps) {
  const timeGrid = useMemo(() => buildMetricsTimeGrid(metrics), [metrics])
  const sections = useMemo(
    () => buildAdminMetricSections(metrics, timeGrid),
    [metrics, timeGrid]
  )

  const hasChartData = sections.length > 0
  const showRangeCallout = !hasChartData

  if (sections.length === 0 && !showRangeCallout) {
    return null
  }

  const rangeLabel = formatMetricTimeWindowDescription(timeWindow)
  const hasCurrentMetrics = chartsHaveData(
    overviewCharts(metrics.overview ?? {})
  )

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
              ? `Metrics are being collected, but the platform hasn't been monitored long enough to fill the ${rangeLabel}. Try a shorter range.`
              : `No chart data is available for the ${rangeLabel}. Try a shorter or more recent time range.`}
          </p>
        </Callout>
      ) : null}
      {adminOverviewHasData(metrics) ? (
        <AdminOverviewStats metrics={metrics} />
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

export { AdminMetricsView }
