import { MetricChartGrid } from "@/components/metrics/metric-chart-grid"
import { chartsHaveData } from "@/lib/metrics/chart-config"
import type { MetricChartConfig } from "@/lib/metrics/chart-config"
import type { LeafInput } from "@/lib/metrics/sections/builder"
import type { MetricsTimeGrid } from "@/lib/metrics/timestamps"

type ChartLeafInput = Omit<LeafInput, "render"> & {
  charts: MetricChartConfig[]
  timeGrid: MetricsTimeGrid
}

type SectionBuilder = {
  leaf: (input: LeafInput) => void
}

function addChartSection(builder: SectionBuilder, input: ChartLeafInput) {
  if (!chartsHaveData(input.charts)) {
    return
  }

  const { charts, timeGrid, ...leaf } = input

  builder.leaf({
    ...leaf,
    render: () => <MetricChartGrid timeGrid={timeGrid} charts={charts} />,
  })
}

export { addChartSection }
export type { ChartLeafInput }
