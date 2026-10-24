import type { ChartThresholdLevel } from "@/lib/metrics/chart-thresholds"
import { PERCENT_THRESHOLDS } from "@/lib/metrics/chart-thresholds"
import { cn } from "@/lib/utils"

export type PercentLevel = ChartThresholdLevel | "normal" | "unknown"

export function getPercentLevel(value: number): PercentLevel {
  let level: PercentLevel = "normal"

  for (const threshold of PERCENT_THRESHOLDS) {
    if (value >= threshold.value) {
      level = threshold.level
    }
  }

  return level
}

const percentLevelColorClasses: Record<PercentLevel, string> = {
  unknown: "text-muted-foreground",
  normal: "text-green-700 dark:text-success",
  warning: "text-warning-700 dark:text-warning",
  critical: "text-error",
}

export function percentLevelColorClass(
  value: number | null,
  className?: string
): string {
  const level = value == null ? "unknown" : getPercentLevel(value)
  return cn(percentLevelColorClasses[level], className)
}

export function fleetOnlineStatusColorClass(
  online: number,
  offline: number
): string | undefined {
  if (online > 0 && offline === 0) {
    return "text-success"
  }

  if (online === 0) {
    return "text-error"
  }

  return undefined
}

export function attentionCountColorClass(count: number): string {
  return count === 0 ? "text-success" : "text-warning-700 dark:text-warning"
}

/** Trend lines stay on-brand while healthy and only change colour when elevated. */
export function percentTrendColorClass(value: number | null): string {
  const level = value == null ? "unknown" : getPercentLevel(value)

  switch (level) {
    case "warning":
      return "text-warning-700 dark:text-warning"
    case "critical":
      return "text-error"
    case "unknown":
      return "text-muted-foreground"
    default:
      return "text-brand"
  }
}
