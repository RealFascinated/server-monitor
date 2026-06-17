import type { ChartThreshold } from "@/lib/metrics/chart-thresholds"
import type { ChartSeries } from "@/lib/metrics/series"
import { hasSeriesData } from "@/lib/metrics/series"
import type { ChartYRange } from "@/lib/metrics/uplot-theme"
import type { MetricChartMode } from "@/components/metrics/metric-chart"

type TooltipSortEntry = {
  value: number
  label: string
  seriesIndex: number
}

type MetricChartConfig = {
  title: string
  description?: string
  series: ChartSeries[]
  valueFormatter?: (value: number) => string
  seriesFormatters?: ((value: number) => string)[]
  yRange?: ChartYRange
  thresholds?: ChartThreshold[]
  rightYRange?: ChartYRange
  rightThresholds?: ChartThreshold[]
  rightValueFormatter?: (value: number) => string
  showCurrentValues?: boolean
  mode?: MetricChartMode
  tooltipColumnSize?: number
  tooltipSort?: (a: TooltipSortEntry, b: TooltipSortEntry) => number
}

function chartsHaveData(charts: MetricChartConfig[]): boolean {
  return charts.some((chart) => hasSeriesData(chart.series))
}

export type { MetricChartConfig, TooltipSortEntry }
export { chartsHaveData }
