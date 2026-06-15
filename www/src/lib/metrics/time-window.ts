import type { MetricTimeRange } from "@/lib/metrics/range"
import {
  METRIC_RANGE_LOOKBACK_SECONDS,
  getMetricRangeOption,
} from "@/lib/metrics/range"

export type MetricTimeWindow =
  | { kind: "preset"; range: MetricTimeRange }
  | { kind: "custom"; from: number; to: number }

export type MetricTimeWindowSearch =
  | { range: MetricTimeRange }
  | { from: number; to: number }

export function metricTimeWindowToEpochWindow(window: MetricTimeWindow): {
  from: number
  to: number
} {
  if (window.kind === "custom") {
    return { from: window.from, to: window.to }
  }

  const to = Math.floor(Date.now() / 1000)
  const from = to - METRIC_RANGE_LOOKBACK_SECONDS[window.range]
  return { from, to }
}

export function metricTimeWindowToSearch(
  window: MetricTimeWindow
): MetricTimeWindowSearch {
  if (window.kind === "custom") {
    return { from: window.from, to: window.to }
  }

  return { range: window.range }
}

export function metricTimeWindowQueryKey(
  window: MetricTimeWindow
): string | number[] {
  if (window.kind === "preset") {
    return window.range
  }

  return [window.from, window.to]
}

const METRIC_TIME_WINDOW_FORMATTER = new Intl.DateTimeFormat(undefined, {
  month: "short",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
})

export function formatMetricTimeWindow(window: MetricTimeWindow): {
  label: string
  shortLabel: string
} {
  if (window.kind === "preset") {
    const option = getMetricRangeOption(window.range)
    return { label: option.label, shortLabel: option.label }
  }

  const fromLabel = METRIC_TIME_WINDOW_FORMATTER.format(
    new Date(window.from * 1000)
  )
  const toLabel = METRIC_TIME_WINDOW_FORMATTER.format(
    new Date(window.to * 1000)
  )
  const label = `${fromLabel} – ${toLabel}`

  return {
    label,
    shortLabel: label,
  }
}

export function formatMetricTimeWindowDescription(
  window: MetricTimeWindow
): string {
  if (window.kind === "preset") {
    return getMetricRangeOption(window.range).label.toLowerCase()
  }

  return formatMetricTimeWindow(window).label.toLowerCase()
}

export function epochToDatetimeLocal(epoch: number): string {
  const date = new Date(epoch * 1000)
  const pad = (value: number) => String(value).padStart(2, "0")

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function datetimeLocalToEpoch(value: string): number {
  return Math.floor(new Date(value).getTime() / 1000)
}

export type CustomMetricTimeWindow = Extract<
  MetricTimeWindow,
  { kind: "custom" }
>

export function defaultCustomMetricTimeWindow(): CustomMetricTimeWindow {
  const to = Math.floor(Date.now() / 1000)
  const from = to - METRIC_RANGE_LOOKBACK_SECONDS["24h"]
  return { kind: "custom", from, to }
}
