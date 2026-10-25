import type uPlot from "uplot"

import type { ChartIncidentSpan } from "@/lib/metrics/chart-annotations"
import type { AlertPreview } from "@/lib/api/user/alerts"

export type PreviewChartData = {
  data: uPlot.AlignedData
  labels: string[]
  spans: ChartIncidentSpan[]
}

function seriesLabel(labels: Record<string, string>, fallback: string): string {
  const entries = Object.entries(labels)
  if (entries.length === 0) return fallback
  return entries.map(([, value]) => value).join(" · ")
}

/**
 * Lines every returned series up on one shared x axis (each may have its own timestamps), converting values with
 * {@code convert}, and flattens the firing intervals of all series into annotation spans.
 */
export function buildPreviewChartData(
  preview: AlertPreview,
  fallbackLabel: string,
  convert: (value: number) => number = (value) => value
): PreviewChartData {
  const timestamps = [
    ...new Set(preview.series.flatMap((series) => series.timestamps)),
  ].sort((a, b) => a - b)
  const index = new Map(timestamps.map((timestamp, i) => [timestamp, i]))

  const columns = preview.series.map((series) => {
    const column: (number | null)[] = new Array<number | null>(
      timestamps.length
    ).fill(null)
    series.timestamps.forEach((timestamp, i) => {
      const value = series.values[i]
      column[index.get(timestamp)!] = value == null ? null : convert(value)
    })
    return column
  })

  return {
    data: [timestamps, ...columns],
    labels: preview.series.map((series) =>
      seriesLabel(series.labels, fallbackLabel)
    ),
    spans: preview.series.flatMap((series) =>
      series.firing.map(({ from, to, ongoing }) => ({ from, to, ongoing }))
    ),
  }
}
