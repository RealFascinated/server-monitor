import { Link } from "@tanstack/react-router"
import { ChevronRight, GripVertical } from "lucide-react"
import { memo } from "react"

import {
  CpuBreakdownTooltip,
  hasCpuBreakdown,
} from "@/components/server/cpu-breakdown-tooltip"
import { ServerStatusDot } from "@/components/server/server-status-badge"
import { UsageBar } from "@/components/server/usage-bar"
import { SimpleTooltip } from "@/components/simple-tooltip"
import type { ServerResponse } from "@/lib/api/user/servers"
import {
  formatUptime,
  formatUptimeDetailed,
  memoryUsagePercent,
} from "@/lib/formatter"
import { defaultMetricRangeSearch } from "@/lib/metrics/default-range"
import { SERVER_DRAG_MIME } from "@/lib/servers/drag"
import { pendingOnlyTooltip } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

type ServerCardProps = {
  server: ServerResponse
  isDragging?: boolean
  editMode?: boolean
  onDragStart?: () => void
  onDragEnd?: () => void
}

function ServerCardUptime({ server }: { server: ServerResponse }) {
  const formatted = formatUptime(server.uptimeSeconds)
  const detailed = formatUptimeDetailed(server.uptimeSeconds)
  const tooltip = detailed ?? pendingOnlyTooltip(server.status)
  const className = cn(
    "shrink-0 text-xs text-muted-foreground tabular-nums",
    server.uptimeSeconds == null && "text-neutral-500"
  )

  if (!tooltip) {
    return <span className={className}>{formatted}</span>
  }

  return (
    <SimpleTooltip content={tooltip}>
      <span className={cn("cursor-help", className)}>{formatted}</span>
    </SimpleTooltip>
  )
}

const ServerCard = memo(
  function ServerCard({
    server,
    isDragging = false,
    editMode = false,
    onDragStart,
    onDragEnd,
  }: ServerCardProps) {
    const memPercent = memoryUsagePercent(
      server.memory?.usage ?? null,
      server.memory?.max ?? null
    )
    const cpuTooltip =
      server.cpu && hasCpuBreakdown(server.cpu) ? (
        <CpuBreakdownTooltip cpu={server.cpu} />
      ) : null

    return (
      <div
        className={cn(
          "flex items-stretch gap-1",
          isDragging && "opacity-40"
        )}
      >
        {editMode ? (
          <button
            type="button"
            draggable
            aria-label={`Move ${server.serverName}`}
            className="flex shrink-0 cursor-grab items-center self-center px-1 text-neutral-400 hover:text-neutral-600 active:cursor-grabbing dark:hover:text-neutral-300"
            onDragStart={(event) => {
              event.dataTransfer.effectAllowed = "move"
              event.dataTransfer.setData(
                SERVER_DRAG_MIME,
                String(server.serverId)
              )
              event.dataTransfer.setData("text/plain", String(server.serverId))
              onDragStart?.()
            }}
            onDragEnd={onDragEnd}
          >
            <GripVertical className="size-4" aria-hidden />
          </button>
        ) : null}
        <Link
          to="/servers/$serverId"
          params={{ serverId: String(server.serverId) }}
          search={defaultMetricRangeSearch()}
          className={cn(
            "flex min-w-0 flex-1 items-center gap-3 rounded-sm border border-neutral-200 bg-card px-3 py-2.5 transition-colors hover:bg-neutral-50 active:bg-neutral-100 dark:border-monitor-gray-300 dark:hover:bg-monitor-gray-200 dark:active:bg-monitor-gray-100"
          )}
        >
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <div className="flex min-w-0 items-center gap-2">
              <ServerStatusDot status={server.status} />
              <span className="min-w-0 flex-1 truncate font-medium text-foreground">
                {server.serverName}
              </span>
              <ServerCardUptime server={server} />
            </div>
            <div className="flex flex-col gap-1.5">
              <UsageBar
                label="CPU"
                value={server.cpu?.percent ?? null}
                status={server.status}
                tooltip={cpuTooltip}
              />
              <UsageBar
                label="RAM"
                value={memPercent}
                status={server.status}
              />
            </div>
          </div>
          <ChevronRight
            className="size-4 shrink-0 text-neutral-400"
            aria-hidden
          />
        </Link>
      </div>
    )
  },
  (prev, next) =>
    prev.server === next.server &&
    prev.isDragging === next.isDragging &&
    prev.editMode === next.editMode
)

export { ServerCard }
