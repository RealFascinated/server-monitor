export const METRIC_REFRESH_INTERVALS = ["10s", "30s", "1m", "never"] as const

export type MetricRefreshInterval = (typeof METRIC_REFRESH_INTERVALS)[number]

export type MetricRefreshIntervalOption = {
  value: MetricRefreshInterval
  label: string
  shortLabel: string
}

export const METRIC_REFRESH_INTERVAL_OPTIONS: MetricRefreshIntervalOption[] = [
  { value: "10s", label: "Every 10 seconds", shortLabel: "10s" },
  { value: "30s", label: "Every 30 seconds", shortLabel: "30s" },
  { value: "1m", label: "Every minute", shortLabel: "1 min" },
  { value: "never", label: "Never", shortLabel: "Never" },
]

const METRIC_REFRESH_INTERVAL_MS: Record<
  Exclude<MetricRefreshInterval, "never">,
  number
> = {
  "10s": 10_000,
  "30s": 30_000,
  "1m": 60_000,
}

const METRIC_REFRESH_INTERVAL_BY_VALUE = Object.fromEntries(
  METRIC_REFRESH_INTERVAL_OPTIONS.map((option) => [option.value, option])
) as Record<MetricRefreshInterval, MetricRefreshIntervalOption>

export function getMetricRefreshIntervalOption(
  value: MetricRefreshInterval
): MetricRefreshIntervalOption {
  return METRIC_REFRESH_INTERVAL_BY_VALUE[value]
}

export function getMetricRefreshIntervalMs(
  value: MetricRefreshInterval
): number | false {
  if (value === "never") {
    return false
  }

  return METRIC_REFRESH_INTERVAL_MS[value]
}

export function parseMetricRefreshInterval(
  value: unknown
): MetricRefreshInterval {
  if (
    typeof value === "string" &&
    METRIC_REFRESH_INTERVALS.includes(value as MetricRefreshInterval)
  ) {
    return value as MetricRefreshInterval
  }

  return "10s"
}
