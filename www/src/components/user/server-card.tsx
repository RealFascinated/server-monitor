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
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import { useServerDrag } from "@/hooks/use-server-drag"
import type { ServerResponse } from "@/lib/api/user/servers"
import {
  formatUptime,
  formatUptimeDetailed,
  memoryUsagePercent,
} from "@/lib/formatter"
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
    server.uptimeSeconds == null && "text-muted-foreground"
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
  function ({
    server,
    isDragging = false,
    editMode = false,
    onDragStart,
    onDragEnd,
  }: ServerCardProps) {
    const { defaultRange } = useMetricDefaultRange()
    const { onDragStart: handleDragStart, onDragEnd: handleDragEnd } =
      useServerDrag(server.serverId, { onDragStart, onDragEnd })
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
        className={cn("flex items-stretch gap-1", isDragging && "opacity-40")}
      >
        {editMode ? (
          <button
            type="button"
            draggable
            aria-label={`Move ${server.serverName}`}
            className="flex shrink-0 cursor-grab items-center self-center px-1 text-muted-foreground hover:text-muted-foreground active:cursor-grabbing dark:hover:text-foreground"
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
          >
            <GripVertical className="size-4" aria-hidden />
          </button>
        ) : null}
        <Link
          to="/servers/$serverId"
          params={{ serverId: String(server.serverId) }}
          search={{ range: defaultRange }}
          className={cn(
            "flex min-w-0 flex-1 items-center gap-3 rounded-sm border border-border bg-card px-3 py-2.5 transition-colors hover:bg-card-hover active:bg-muted"
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
              <UsageBar label="RAM" value={memPercent} status={server.status} />
            </div>
          </div>
          <ChevronRight
            className="size-4 shrink-0 text-muted-foreground"
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
