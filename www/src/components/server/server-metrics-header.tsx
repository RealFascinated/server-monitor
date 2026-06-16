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

type ServerMetricsToolbarProps = {
  serverId: number
  timeWindow: MetricTimeWindow
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing: boolean
  onTimeWindowChange: (value: MetricTimeWindow) => void
  showSettings?: boolean
}

function ServerMetricsToolbar({
  serverId,
  timeWindow,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing,
  onTimeWindowChange,
  showSettings = true,
}: ServerMetricsToolbarProps) {
  return (
    <div
      className={metricRangeShellClassName}
      role="toolbar"
      aria-label="Server actions"
    >
      <SimpleTooltip content="Incident history">
        <Button
          variant="ghost"
          size="sm"
          className="h-7 shrink-0 gap-1.5 rounded-sm border-0 px-2 text-xs hover:bg-card/70 dark:hover:bg-accent/60"
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

      {showSettings ? (
        <SimpleTooltip content="Server settings">
          <Button
            variant="ghost"
            size="sm"
            className="h-7 shrink-0 gap-1.5 rounded-sm border-0 px-2 text-xs hover:bg-card/70 dark:hover:bg-accent/60"
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
              <SimpleTooltip content="Incident history">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 shrink-0 rounded-sm border border-border bg-muted/80 px-2.5 text-xs hover:bg-card/70 dark:bg-muted/60 dark:hover:bg-muted"
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
                  className="h-7 shrink-0 rounded-sm border border-border bg-muted/80 px-2.5 text-xs hover:bg-card/70 dark:bg-muted/60 dark:hover:bg-muted"
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
            </div>
          </>
        }
      />

      <div className="sticky top-14 z-30 mb-6 w-full bg-background/95 py-1.5 backdrop-blur-sm lg:hidden">
        <ServerMetricsToolbar {...toolbarProps} showSettings={false} />
      </div>
    </>
  )
}

export { ServerMetricsHeader }
