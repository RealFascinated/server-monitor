import { useNavigate } from "@tanstack/react-router"

import { ServerDetailHeader } from "@/components/server/server-detail-header"
import { LiveIndicator } from "@/components/server/live-indicator"
import { MetricRangeSelector } from "@/components/server/metric-range-selector"
import type { ServerResponse } from "@/lib/api/user/servers"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { metricTimeWindowToSearch } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"

type ServerMetricsHeaderProps = {
  server: ServerResponse | undefined
  timeWindow: MetricTimeWindow
  serverId: number
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing?: boolean
  dataUpdatedAt?: number
}

const metricRangeShellClassName =
  "inline-flex w-full shrink-0 items-stretch gap-0.5 overflow-hidden rounded-md border border-border bg-muted/80 p-1 sm:w-auto dark:bg-muted/60"

type ServerMetricsToolbarProps = {
  timeWindow: MetricTimeWindow
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing: boolean
  dataUpdatedAt: number
  onTimeWindowChange: (value: MetricTimeWindow) => void
}

function ServerMetricsToolbar({
  timeWindow,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing,
  dataUpdatedAt,
  onTimeWindowChange,
}: ServerMetricsToolbarProps) {
  return (
    <div
      className={metricRangeShellClassName}
      role="toolbar"
      aria-label="Server actions"
    >
      {timeWindow.kind === "preset" ? (
        <LiveIndicator
          updatedAt={dataUpdatedAt}
          refreshInterval={refreshInterval}
          isRefreshing={isRefreshing}
          onRefreshIntervalChange={onRefreshIntervalChange}
        />
      ) : null}

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

function ServerMetricsHeader({
  server,
  timeWindow,
  serverId,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing = false,
  dataUpdatedAt = 0,
}: ServerMetricsHeaderProps) {
  const navigate = useNavigate()

  function handleTimeWindowChange(nextWindow: MetricTimeWindow) {
    navigate({
      to: "/servers/$serverId",
      params: { serverId: String(serverId) },
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
    dataUpdatedAt,
    onTimeWindowChange: handleTimeWindowChange,
  }

  return (
    <>
      <ServerDetailHeader
        server={server}
        serverId={serverId}
        sticky
        className="mb-2.5 lg:mb-6"
        actions={
          <div className="hidden shrink-0 lg:block">
            <ServerMetricsToolbar {...toolbarProps} />
          </div>
        }
      />

      <div className="sticky top-14 z-30 mb-6 w-full bg-background/95 py-1.5 backdrop-blur-sm lg:hidden">
        <ServerMetricsToolbar {...toolbarProps} />
      </div>
    </>
  )
}

export { ServerMetricsHeader }
