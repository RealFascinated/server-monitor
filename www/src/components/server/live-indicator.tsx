import { Pause, Play } from "lucide-react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { useLiveNow } from "@/hooks/use-live-now"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { cn } from "@/lib/utils"

type LiveIndicatorProps = {
  /** `dataUpdatedAt` from the metrics query (ms), or 0 before the first load. */
  updatedAt: number
  refreshInterval: MetricRefreshInterval
  isRefreshing: boolean
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
}

const RESUME_INTERVAL: MetricRefreshInterval = "10s"

function formatAge(updatedAt: number, now: number): string {
  if (updatedAt <= 0) {
    return "loading"
  }

  const seconds = Math.max(0, Math.round((now - updatedAt) / 1000))
  if (seconds < 5) return "just now"
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  return `${Math.floor(minutes / 60)}h ago`
}

function LiveIndicator({
  updatedAt,
  refreshInterval,
  isRefreshing,
  onRefreshIntervalChange,
}: LiveIndicatorProps) {
  const now = useLiveNow({ enabled: true, intervalMs: 1_000 })
  const paused = refreshInterval === "never"
  const Icon = paused ? Play : Pause
  const action = paused ? "Resume live updates" : "Pause live updates"

  return (
    <SimpleTooltip content={action}>
      <button
        type="button"
        aria-label={`${action}. Last updated ${formatAge(updatedAt, now)}.`}
        onClick={() =>
          onRefreshIntervalChange(paused ? RESUME_INTERVAL : "never")
        }
        className="group/live inline-flex h-7 shrink-0 items-center gap-1.5 rounded-md px-2 text-xs font-medium text-muted-foreground transition-colors hover:bg-card/70 hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none dark:hover:bg-accent/60"
      >
        <span
          aria-hidden
          className={cn(
            "size-1.5 rounded-full",
            paused ? "bg-muted-foreground" : "bg-success",
            !paused && !isRefreshing && "status-pulse-dot"
          )}
        />
        <span className="tabular-nums">
          {paused ? "Paused" : "Live"} · {formatAge(updatedAt, now)}
        </span>
        <Icon
          aria-hidden
          className="size-3 opacity-0 transition-opacity group-hover/live:opacity-100 group-focus-visible/live:opacity-100"
        />
      </button>
    </SimpleTooltip>
  )
}

export { LiveIndicator }
