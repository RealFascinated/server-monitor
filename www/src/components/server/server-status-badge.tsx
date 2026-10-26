import { CircleDashed, CircleX } from "lucide-react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { useLiveNow } from "@/hooks/use-live-now"
import type { ServerStatus } from "@/lib/api/user/servers"
import {
  formatRelativeTime,
  formatTimestampWithRelative,
} from "@/lib/formatter"
import { SERVER_STATUS_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

type ServerStatusIndicatorProps = {
  status: ServerStatus
  lastHeartbeat?: string | null
}

const statusDotStyles: Record<ServerStatus, string> = {
  ONLINE: "bg-green-500",
  OFFLINE: "bg-red-500",
  PENDING: "bg-amber-500",
}

const statusStyles: Record<ServerStatus, string> = {
  ONLINE:
    "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300",
  OFFLINE: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
  PENDING:
    "bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300",
}

function statusTooltip(
  status: ServerStatus,
  lastHeartbeat: string | null
): string {
  if (status === "OFFLINE" && lastHeartbeat) {
    return `Last heartbeat ${formatTimestampWithRelative(lastHeartbeat)}.`
  }

  return SERVER_STATUS_TOOLTIPS[status]
}

/**
 * Status marker that differs by shape as well as colour: a filled pulsing dot
 * (online), a crossed circle (offline) and a dashed circle (pending).
 */
function ServerStatusDot({
  status,
  lastHeartbeat = null,
}: ServerStatusIndicatorProps) {
  const className = "shrink-0 cursor-help"

  return (
    <SimpleTooltip content={statusTooltip(status, lastHeartbeat)}>
      {status === "ONLINE" ? (
        <span
          role="img"
          aria-label={status}
          className={cn(
            className,
            "mx-0.5 size-2.5 rounded-full",
            statusDotStyles[status],
            "status-pulse-dot"
          )}
        />
      ) : (
        <span
          role="img"
          aria-label={status}
          className={cn(className, "inline-flex")}
        >
          {status === "OFFLINE" ? (
            <CircleX aria-hidden className="size-3.5 text-red-500" />
          ) : (
            <CircleDashed aria-hidden className="size-3.5 text-amber-500" />
          )}
        </span>
      )}
    </SimpleTooltip>
  )
}

function ServerStatusBadge({
  status,
  lastHeartbeat = null,
}: ServerStatusIndicatorProps) {
  const showLastHeartbeat = status === "OFFLINE" && lastHeartbeat != null
  const now = useLiveNow({ enabled: showLastHeartbeat })
  const relativeHeartbeat =
    showLastHeartbeat && lastHeartbeat
      ? formatRelativeTime(lastHeartbeat, now)
      : null

  return (
    <SimpleTooltip content={statusTooltip(status, lastHeartbeat)}>
      <span className="inline-flex cursor-help items-center gap-1.5">
        <span
          className={cn(
            "inline-flex items-center gap-1.5 rounded-sm px-2 py-0.5 text-xs font-medium",
            statusStyles[status]
          )}
        >
          {status === "ONLINE" ? (
            <span
              aria-hidden
              className="status-pulse-dot size-1.5 shrink-0 rounded-full bg-current"
            />
          ) : null}
          {status}
        </span>
        {relativeHeartbeat ? (
          <span className="text-xs whitespace-nowrap text-muted-foreground tabular-nums">
            {relativeHeartbeat}
          </span>
        ) : null}
      </span>
    </SimpleTooltip>
  )
}

export { ServerStatusBadge, ServerStatusDot }
