import { Link, useNavigate } from "@tanstack/react-router"
import { History, Settings } from "lucide-react"

import { ServerDetailHeader } from "@/components/server/server-detail-header"
import { MetricRangeSelector } from "@/components/server/metric-range-selector"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Button } from "@/components/ui/button"
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
}

const metricRangeShellClassName =
  "inline-flex w-full shrink-0 items-stretch gap-0.5 overflow-hidden rounded-sm border border-border bg-muted/80 p-1 sm:w-auto dark:bg-muted/60"

type ServerMetricsNavLinksProps = {
  serverId: number
  variant: "toolbar" | "header"
}

function ServerMetricsNavLinks({ serverId, variant }: ServerMetricsNavLinksProps) {
  const buttonClass =
    variant === "toolbar"
      ? "h-7 shrink-0 gap-1.5 rounded-sm border-0 px-2 text-xs hover:bg-card/70 dark:hover:bg-accent/60"
      : "h-7 shrink-0 rounded-sm border border-border bg-muted/80 px-2.5 text-xs hover:bg-card/70 dark:bg-muted/60 dark:hover:bg-muted"

  return (
    <>
      <SimpleTooltip content="Incident history">
        <Button
          variant="ghost"
          size="sm"
          className={buttonClass}
          asChild
        >
          <Link
            to="/servers/$serverId/incidents"
            params={{ serverId: String(serverId) }}
          >
            <History className="size-3.5" />
            <span className="hidden sm:inline">Incidents</span>
          </Link>
        </Button>
      </SimpleTooltip>

      <SimpleTooltip content="Server settings">
        <Button
          variant="ghost"
          size="sm"
          className={buttonClass}
          asChild
        >
          <Link
            to="/servers/$serverId/settings"
            params={{ serverId: String(serverId) }}
          >
            <Settings className="size-3.5" />
            <span className="hidden sm:inline">Settings</span>
          </Link>
        </Button>
      </SimpleTooltip>
    </>
  )
}

type ServerMetricsToolbarProps = {
  serverId: number
  timeWindow: MetricTimeWindow
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing: boolean
  onTimeWindowChange: (value: MetricTimeWindow) => void
  showNav?: boolean
}

function ServerMetricsToolbar({
  serverId,
  timeWindow,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing,
  onTimeWindowChange,
  showNav = true,
}: ServerMetricsToolbarProps) {
  return (
    <div
      className={metricRangeShellClassName}
      role="toolbar"
      aria-label="Server actions"
    >
      {showNav ? <ServerMetricsNavLinks serverId={serverId} variant="toolbar" /> : null}

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
    serverId,
    timeWindow,
    refreshInterval,
    onRefreshIntervalChange,
    onRefresh,
    isRefreshing,
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
          <>
            <div className="hidden shrink-0 lg:block">
              <ServerMetricsToolbar {...toolbarProps} />
            </div>

            <div className="flex shrink-0 items-center gap-1 lg:hidden">
              <ServerMetricsNavLinks serverId={serverId} variant="header" />
            </div>
          </>
        }
      />

      <div className="sticky top-14 z-30 mb-6 w-full bg-background/95 py-1.5 backdrop-blur-sm lg:hidden">
        <ServerMetricsToolbar {...toolbarProps} showNav={false} />
      </div>
    </>
  )
}

export { ServerMetricsHeader }
