import {
  DEFAULT_METRIC_TIME_RANGE,
  parseMetricRange,
} from "@/lib/metrics/range"
import type { MetricTimeRange } from "@/lib/metrics/range"

export { DEFAULT_METRIC_TIME_RANGE }

export const METRIC_DEFAULT_RANGE_STORAGE_KEY = "metric-default-range"

export function getStoredDefaultMetricRange(): MetricTimeRange {
  if (typeof window === "undefined") {
    return DEFAULT_METRIC_TIME_RANGE
  }

  return parseMetricRange(
    localStorage.getItem(METRIC_DEFAULT_RANGE_STORAGE_KEY)
  )
}

export function defaultMetricRangeSearch() {
  return { range: getStoredDefaultMetricRange() }
}
