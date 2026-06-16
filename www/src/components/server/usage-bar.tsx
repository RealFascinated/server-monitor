import type { ReactNode } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import type { ServerStatus } from "@/lib/api/user/servers"
import { formatPercent } from "@/lib/formatter"
import type { PercentLevel } from "@/lib/metrics/percent-level"
import { getPercentLevel, percentLevelColorClass } from "@/lib/metrics/percent-level"
import { PENDING_NO_METRIC_DATA } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

const barFillClasses: Record<PercentLevel, string> = {
  unknown: "bg-neutral-400 dark:bg-neutral-500",
  normal: "bg-[#2E9470] dark:bg-green-400",
  warning: "bg-[#B8870A] dark:bg-warning",
  critical: "bg-[#C44E4E] dark:bg-error",
}

function UsageBar({
  label,
  value,
  status,
  tooltip,
}: {
  label: string
  value: number | null
  status?: ServerStatus
  tooltip?: ReactNode
}) {
  const level = value == null ? "unknown" : getPercentLevel(value)
  const width = value == null ? 0 : Math.min(100, Math.max(0, value))
  const pendingTooltip =
    value == null && status === "PENDING" ? PENDING_NO_METRIC_DATA : null
  const content = tooltip ?? pendingTooltip

  const bar = (
    <div className="flex min-w-0 items-center gap-2">
      <span className="w-8 shrink-0 text-xs text-muted-foreground">{label}</span>
      <div className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-neutral-200 dark:bg-monitor-gray-300">
        <div
          className={cn("h-full rounded-full", barFillClasses[level])}
          style={{ width: `${width}%` }}
        />
      </div>
      <span
        className={cn(
          "w-10 shrink-0 text-right text-xs font-medium tabular-nums",
          percentLevelColorClass(value)
        )}
      >
        {formatPercent(value)}
      </span>
    </div>
  )

  if (!content) {
    return bar
  }

  return <SimpleTooltip content={content}>{bar}</SimpleTooltip>
}

export { UsageBar }
