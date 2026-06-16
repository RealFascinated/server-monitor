import { Link } from "@tanstack/react-router"
import { Server } from "lucide-react"
import { memo, useCallback } from "react"
import { useQueryClient } from "@tanstack/react-query"

import { CpuPercent, MemoryPercent } from "@/components/server/usage-percent"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import { useSidebarColumnVisibility } from "@/hooks/use-sidebar-column-visibility"
import { userServerMetricsQueryOptions } from "@/lib/api/user/metrics.queries"
import { resolveUserServerFromCache } from "@/lib/api/user/servers.queries"
import type { ServerResponse, ServerStatus } from "@/lib/api/user/servers"
import { SERVER_STATUS_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

const statusDotStyles: Record<ServerStatus, string> = {
  ONLINE: "bg-green-500",
  OFFLINE: "bg-red-500",
  PENDING: "bg-amber-500",
}

export const SidebarServerItem = memo(
  function ({
    server,
    compact,
    detailed,
    onNavigate,
    nested = false,
  }: {
    server: ServerResponse
    compact: boolean
    detailed: boolean
    onNavigate?: () => void
    nested?: boolean
  }) {
    const { visibility } = useSidebarColumnVisibility()
    const { defaultRange } = useMetricDefaultRange()
    const queryClient = useQueryClient()

    const prefetchServer = useCallback(() => {
      resolveUserServerFromCache(queryClient, server.serverId)
      void queryClient.prefetchQuery(
        userServerMetricsQueryOptions(server.serverId, {
          kind: "preset",
          range: defaultRange,
        })
      )
    }, [queryClient, server.serverId, defaultRange])

    const serverTooltip = `${server.serverName} — ${SERVER_STATUS_TOOLTIPS[server.status]}`

    const link = (
      <Link
        to="/servers/$serverId"
        params={{ serverId: String(server.serverId) }}
        search={{ range: defaultRange }}
        onClick={onNavigate}
        onMouseEnter={prefetchServer}
        onFocus={prefetchServer}
        className={cn(
          "flex w-full shrink-0 rounded-sm text-sm font-medium text-muted-foreground transition-colors hover:bg-muted",
          "[&.active]:bg-muted [&.active]:text-foreground dark:[&.active]:text-warning",
          nested && !compact && "pl-4",
          compact
            ? "min-h-7 cursor-pointer items-center justify-center gap-3 px-0 py-1"
            : detailed
              ? "items-center gap-2 px-2 py-1"
              : "min-h-7 items-center gap-3 px-2 py-1"
        )}
      >
        <span className="relative shrink-0">
          <Server className="size-4" />
          <span
            className={cn(
              "absolute -right-0.5 -bottom-0.5 size-1.5 rounded-full ring-2 ring-white dark:ring-base",
              statusDotStyles[server.status]
            )}
          />
        </span>
        {!compact ? (
          <span
            className={cn(
              "flex min-w-0 flex-1 flex-col",
              detailed ? "gap-0 leading-tight" : "gap-0.5"
            )}
          >
            <span className="truncate leading-tight">{server.serverName}</span>
            {detailed && (visibility.cpu || visibility.ram) ? (
              <span className="truncate text-[11px] leading-tight text-muted-foreground">
                {visibility.cpu ? (
                  <>
                    CPU{" "}
                    <CpuPercent
                      cpu={server.cpu}
                      status={server.status}
                      className="font-medium"
                    />
                  </>
                ) : null}
                {visibility.cpu && visibility.ram ? " · " : null}
                {visibility.ram ? (
                  <>
                    RAM{" "}
                    <MemoryPercent
                      usage={server.memory?.usage ?? null}
                      max={server.memory?.max ?? null}
                      status={server.status}
                      className="font-medium"
                    />
                  </>
                ) : null}
              </span>
            ) : null}
          </span>
        ) : null}
      </Link>
    )

    if (compact) {
      return <SimpleTooltip content={serverTooltip}>{link}</SimpleTooltip>
    }

    return link
  },
  (prev, next) =>
    prev.server === next.server &&
    prev.compact === next.compact &&
    prev.detailed === next.detailed &&
    prev.nested === next.nested
)
