import type { ReactElement } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import type { ServerStatus } from "@/lib/api/user/servers"
import {
  formatMemoryBytes,
  formatPercent,
  memoryUsagePercent,
} from "@/lib/formatter"
import { PENDING_NO_METRIC_DATA } from "@/lib/tooltips/copy"
import { percentLevelColorClass } from "@/lib/metrics/percent-level"

function ColoredPercent({
  value,
  className,
}: {
  value: number | null
  className?: string
}) {
  return (
    <span className={percentLevelColorClass(value, className)}>
      {formatPercent(value)}
    </span>
  )
}

function withMetricTooltip(content: string | null, node: ReactElement) {
  if (!content) {
    return node
  }

  return (
    <SimpleTooltip content={content}>
      <span className="cursor-help">{node}</span>
    </SimpleTooltip>
  )
}

function getCpuTooltip(
  value: number | null,
  status?: ServerStatus
): string | null {
  if (value != null || status !== "PENDING") {
    return null
  }

  return PENDING_NO_METRIC_DATA
}

function getUsageTooltip(
  usage: number | null,
  max: number | null,
  status?: ServerStatus
): string | null {
  if (usage != null && max != null && max > 0) {
    return `${formatMemoryBytes(usage)} of ${formatMemoryBytes(max)}`
  }

  if (status === "PENDING") {
    return PENDING_NO_METRIC_DATA
  }

  return null
}

function CpuPercent({
  value,
  status,
  className,
}: {
  value: number | null
  status?: ServerStatus
  className?: string
}) {
  return withMetricTooltip(
    getCpuTooltip(value, status),
    <ColoredPercent value={value} className={className} />
  )
}

function UsagePercent({
  usage,
  max,
  status,
  className,
}: {
  usage: number | null
  max: number | null
  status?: ServerStatus
  className?: string
}) {
  const percent = memoryUsagePercent(usage, max)

  return withMetricTooltip(
    getUsageTooltip(usage, max, status),
    <ColoredPercent value={percent} className={className} />
  )
}

function MemoryPercent({
  usage,
  max,
  status,
  className,
}: {
  usage: number | null
  max: number | null
  status?: ServerStatus
  className?: string
}) {
  return (
    <UsagePercent
      usage={usage}
      max={max}
      status={status}
      className={className}
    />
  )
}

function DiskPercent({
  usage,
  max,
  status,
  className,
}: {
  usage: number | null
  max: number | null
  status?: ServerStatus
  className?: string
}) {
  return (
    <UsagePercent
      usage={usage}
      max={max}
      status={status}
      className={className}
    />
  )
}

export { CpuPercent, DiskPercent, MemoryPercent }
