export type MetricTimeRange =
  | "1h"
  | "3h"
  | "6h"
  | "12h"
  | "24h"
  | "3d"
  | "7d"
  | "2w"
  | "1mo"
  | "3mo"
  | "1y"
  | "2y"

/** Lookback durations in seconds — must match API MetricTimeRange. */
export const METRIC_RANGE_LOOKBACK_SECONDS: Record<MetricTimeRange, number> = {
  "1h": 3_600,
  "3h": 10_800,
  "6h": 21_600,
  "12h": 43_200,
  "24h": 86_400,
  "3d": 259_200,
  "7d": 604_800,
  "2w": 1_209_600,
  "1mo": 2_592_000,
  "3mo": 7_776_000,
  "1y": 31_536_000,
  "2y": 63_072_000,
}

export const METRIC_RANGES = [
  "1h",
  "3h",
  "6h",
  "12h",
  "24h",
  "3d",
  "7d",
  "2w",
  "1mo",
  "3mo",
  "1y",
  "2y",
] as const satisfies readonly MetricTimeRange[]

export const DEFAULT_METRIC_TIME_RANGE: MetricTimeRange = "7d"

export type MetricRangeGroup = "hours" | "days" | "weeks" | "months" | "years"

export type MetricRangeOption = {
  value: MetricTimeRange
  label: string
  shortLabel: string
  group: MetricRangeGroup
}

export const METRIC_RANGE_GROUPS: {
  id: MetricRangeGroup
  label: string
}[] = [
  { id: "hours", label: "Hours" },
  { id: "days", label: "Days" },
  { id: "weeks", label: "Weeks" },
  { id: "months", label: "Months" },
  { id: "years", label: "Years" },
]

export const METRIC_RANGE_OPTIONS: MetricRangeOption[] = [
  { value: "1h", label: "Last hour", shortLabel: "1h", group: "hours" },
  { value: "3h", label: "Last 3 hours", shortLabel: "3h", group: "hours" },
  { value: "6h", label: "Last 6 hours", shortLabel: "6h", group: "hours" },
  { value: "12h", label: "Last 12 hours", shortLabel: "12h", group: "hours" },
  { value: "24h", label: "Last 24 hours", shortLabel: "24h", group: "hours" },
  { value: "3d", label: "Last 3 days", shortLabel: "3d", group: "days" },
  { value: "7d", label: "Last 7 days", shortLabel: "7d", group: "days" },
  { value: "2w", label: "Last 2 weeks", shortLabel: "2w", group: "weeks" },
  { value: "1mo", label: "Last 30 days", shortLabel: "30d", group: "months" },
  { value: "3mo", label: "Last 3 months", shortLabel: "3mo", group: "months" },
  { value: "1y", label: "Last year", shortLabel: "1y", group: "years" },
  { value: "2y", label: "Last 2 years", shortLabel: "2y", group: "years" },
]

const METRIC_RANGE_BY_VALUE = Object.fromEntries(
  METRIC_RANGE_OPTIONS.map((option) => [option.value, option])
) as Record<MetricTimeRange, MetricRangeOption>

export function getMetricRangeOption(
  value: MetricTimeRange
): MetricRangeOption {
  return METRIC_RANGE_BY_VALUE[value]
}

export function parseMetricRange(value: unknown): MetricTimeRange {
  if (
    typeof value === "string" &&
    METRIC_RANGES.includes(value as MetricTimeRange)
  ) {
    return value as MetricTimeRange
  }

  return DEFAULT_METRIC_TIME_RANGE
}
