import {
  Coffee,
  Database,
  Globe,
  LayoutDashboard,
  Monitor,
  Package,
  Server,
  Upload,
} from "lucide-react"

import type { AdminMetricsResponse } from "@/lib/api/admin/metrics"
import { createMetricsSectionBuilder } from "@/lib/metrics/sections/builder"
import { addChartSection } from "@/lib/metrics/sections/chart-section"
import type { MetricsSectionNode } from "@/lib/metrics/sections/types"
import {
  fleetCharts,
  fleetOsCharts,
  fleetVersionCharts,
  httpCharts,
  ingestCharts,
  jvmCharts,
  overviewCharts,
  parseHttpEntries,
  vmCharts,
} from "@/lib/metrics/sections/admin/charts"
import type { MetricsTimeGrid } from "@/lib/metrics/timestamps"

function buildAdminMetricSections(
  metrics: AdminMetricsResponse,
  timeGrid: MetricsTimeGrid
): MetricsSectionNode[] {
  const builder = createMetricsSectionBuilder()

  addChartSection(builder, {
    title: "Overview",
    icon: LayoutDashboard,
    charts: overviewCharts(metrics.overview ?? {}),
    timeGrid,
  })

  addChartSection(builder, {
    title: "Fleet",
    icon: Server,
    charts: fleetCharts(metrics.fleet ?? {}),
    timeGrid,
  })

  addChartSection(builder, {
    title: "OS breakdown",
    icon: Monitor,
    charts: fleetOsCharts(metrics.fleet?.byOs ?? []),
    timeGrid,
  })

  addChartSection(builder, {
    title: "Agent versions",
    icon: Package,
    charts: fleetVersionCharts(metrics.fleet?.byAgentVersion ?? []),
    timeGrid,
  })

  addChartSection(builder, {
    title: "Ingest",
    icon: Upload,
    charts: ingestCharts(metrics.ingest ?? {}),
    timeGrid,
  })

  addChartSection(builder, {
    title: "JVM",
    icon: Coffee,
    charts: jvmCharts(metrics.jvm ?? {}),
    timeGrid,
  })

  addChartSection(builder, {
    title: "VictoriaMetrics",
    icon: Database,
    charts: vmCharts(metrics.vm ?? {}),
    timeGrid,
  })

  addChartSection(builder, {
    title: "HTTP",
    icon: Globe,
    charts: httpCharts(parseHttpEntries(metrics.http)),
    timeGrid,
  })

  return builder.build()
}

export { buildAdminMetricSections }
