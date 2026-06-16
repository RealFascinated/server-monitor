import { useNavigate } from "@tanstack/react-router"
import { Gauge } from "lucide-react"

import { PageHeader } from "@/components/page-header"
import { MetricRangeSelector } from "@/components/server/metric-range-selector"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { metricTimeWindowToSearch } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"

type AdminMetricsHeaderProps = {
  timeWindow: MetricTimeWindow
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing?: boolean
}

const metricRangeShellClassName =
  "inline-flex w-full shrink-0 items-stretch overflow-hidden rounded-sm border border-neutral-200 bg-neutral-100/80 p-1 sm:w-auto dark:border-monitor-gray-300 dark:bg-monitor-gray-200/60"

type AdminMetricsToolbarProps = {
  timeWindow: MetricTimeWindow
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing: boolean
  onTimeWindowChange: (value: MetricTimeWindow) => void
}

function AdminMetricsToolbar({
  timeWindow,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing,
  onTimeWindowChange,
}: AdminMetricsToolbarProps) {
  return (
    <div
      className={metricRangeShellClassName}
      role="toolbar"
      aria-label="Time range"
    >
      <MetricRangeSelector
        value={timeWindow}
        onChange={onTimeWindowChange}
        refreshInterval={refreshInterval}
        onRefreshIntervalChange={onRefreshIntervalChange}
        onRefresh={onRefresh}
        isRefreshing={isRefreshing}
      />
    </div>
  )
}

function AdminMetricsHeader({
  timeWindow,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing = false,
}: AdminMetricsHeaderProps) {
  const navigate = useNavigate()

  function handleTimeWindowChange(nextWindow: MetricTimeWindow) {
    navigate({
      to: "/admin/metrics",
      search: metricTimeWindowToSearch(nextWindow),
      resetScroll: false,
    })
  }

  const toolbarProps = {
    timeWindow,
    refreshInterval,
    onRefreshIntervalChange,
    onRefresh,
    isRefreshing,
    onTimeWindowChange: handleTimeWindowChange,
  }

  return (
    <>
      <PageHeader
        sticky
        className="mb-2.5 lg:mb-6"
        breadcrumb={[
          { label: "Servers", to: "/" },
          { label: "Admin Metrics", current: true },
        ]}
        icon={Gauge}
        title="Admin Metrics"
        description="Platform-wide metrics for fleet health, ingest, JVM, VictoriaMetrics, and HTTP traffic."
        actions={
          <div className="hidden shrink-0 lg:block">
            <AdminMetricsToolbar {...toolbarProps} />
          </div>
        }
      />

      <div className="sticky top-14 z-30 mb-6 w-full bg-background/95 py-1.5 backdrop-blur-sm lg:hidden">
        <AdminMetricsToolbar {...toolbarProps} />
      </div>
    </>
  )
}

export { AdminMetricsHeader }
