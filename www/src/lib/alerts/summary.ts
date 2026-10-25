import type {
  AlertOperator,
  AlertSeverity,
  AlertUnit,
} from "@/lib/api/user/alerts"
import { formatAlertValue } from "@/lib/alerts/units"
import type { TemperatureUnit } from "@/lib/metrics/temperature"

export const OPERATOR_LABELS: Record<AlertOperator, string> = {
  MORE_THAN: "above",
  LESS_THAN: "below",
}

export const SEVERITY_LABELS: Record<AlertSeverity, string> = {
  WARNING: "Warning",
  CRITICAL: "Critical",
}

export const WINDOW_OPTIONS = [
  { value: 0, label: "Instant" },
  { value: 60, label: "1 minute" },
  { value: 300, label: "5 minutes" },
  { value: 600, label: "10 minutes" },
  { value: 1800, label: "30 minutes" },
] as const

export const REPEAT_OPTIONS = [
  { value: 0, label: "Never" },
  { value: 900, label: "Every 15 minutes" },
  { value: 3600, label: "Every hour" },
  { value: 14_400, label: "Every 4 hours" },
  { value: 86_400, label: "Every 24 hours" },
] as const

/** "5 minutes", "1 hour", "90 seconds". */
export function formatWindow(seconds: number): string {
  if (seconds <= 0) return "instantly"
  if (seconds % 3600 === 0) {
    const hours = seconds / 3600
    return hours === 1 ? "1 hour" : `${hours} hours`
  }
  if (seconds % 60 === 0) {
    const minutes = seconds / 60
    return minutes === 1 ? "1 minute" : `${minutes} minutes`
  }
  return `${seconds} seconds`
}

export type ConditionInput = {
  metricLabel: string
  unit: AlertUnit
  operator: AlertOperator
  threshold: number
  windowSeconds: number
  labelFilter?: Record<string, string>
}

export function describeLabelFilter(
  labelFilter: Record<string, string> | undefined
): string | null {
  const entries = Object.entries(labelFilter ?? {})
  if (entries.length === 0) return null
  return entries.map(([key, value]) => `${key}: ${value}`).join(", ")
}

/** "CPU usage, averaged over 5 minutes, is above 90%". */
export function describeCondition(
  input: ConditionInput,
  temperatureUnit: TemperatureUnit
): string {
  if (input.unit === "STATE") {
    return `${input.metricLabel} is unhealthy`
  }

  const averaged =
    input.windowSeconds > 0
      ? `, averaged over ${formatWindow(input.windowSeconds)},`
      : ""
  const value = formatAlertValue(input.unit, input.threshold, temperatureUnit)

  return `${input.metricLabel}${averaged} is ${OPERATOR_LABELS[input.operator]} ${value}`
}
