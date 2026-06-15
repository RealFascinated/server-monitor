import type { ChartThreshold } from "@/lib/metrics/chart-thresholds"
import type { ChartSeries } from "@/lib/metrics/series"
import { hasSeriesData } from "@/lib/metrics/series"
import type { ChartYRange } from "@/lib/metrics/uplot-theme"
import type { MetricChartMode } from "@/components/metrics/metric-chart"

type MetricChartConfig = {
  title: string
  description?: string
  series: ChartSeries[]
  valueFormatter?: (value: number) => string
  seriesFormatters?: ((value: number) => string)[]
  yRange?: ChartYRange
  thresholds?: ChartThreshold[]
  showCurrentValues?: boolean
  mode?: MetricChartMode
}

function chartsHaveData(charts: MetricChartConfig[]): boolean {
  return charts.some((chart) => hasSeriesData(chart.series))
}

export type { MetricChartConfig }
export { chartsHaveData }
