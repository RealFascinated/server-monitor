import { SimpleTooltip } from "@/components/simple-tooltip"
import type { ServerStatus } from "@/lib/api/user/servers"
import { SERVER_STATUS_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

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

function ServerStatusDot({ status }: { status: ServerStatus }) {
  return (
    <SimpleTooltip content={SERVER_STATUS_TOOLTIPS[status]}>
      <span
        aria-label={status}
        className={cn(
          "size-2 shrink-0 cursor-help rounded-full",
          statusDotStyles[status],
          status === "ONLINE" && "status-pulse-dot"
        )}
      />
    </SimpleTooltip>
  )
}

function ServerStatusBadge({ status }: { status: ServerStatus }) {
  return (
    <SimpleTooltip content={SERVER_STATUS_TOOLTIPS[status]}>
      <span
        className={cn(
          "inline-flex cursor-help items-center gap-1.5 rounded-sm px-2 py-0.5 text-xs font-medium",
          statusStyles[status]
        )}
      >
        {status === "ONLINE" ? (
          <span
            aria-hidden
            className="size-1.5 shrink-0 rounded-full bg-current status-pulse-dot"
          />
        ) : null}
        {status}
      </span>
    </SimpleTooltip>
  )
}

export { ServerStatusBadge, ServerStatusDot }
