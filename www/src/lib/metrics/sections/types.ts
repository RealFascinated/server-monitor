import type { LucideIcon } from "lucide-react"
import type { ReactNode } from "react"

type MetricsSectionLeaf = {
  kind: "leaf"
  id: string
  title: string
  sectionKind?: string
  navLabel?: string
  navPercent?: number | null
  navPercentTooltip?: string
  icon: LucideIcon
  description?: string
  render: () => ReactNode
}

type MetricsSectionGroup = {
  kind: "group"
  id: string
  title: string
  icon: LucideIcon
  showChildCount?: boolean
  children: MetricsSectionNode[]
}

type MetricsSectionNode = MetricsSectionLeaf | MetricsSectionGroup

function isMetricsSectionGroup(
  node: MetricsSectionNode
): node is MetricsSectionGroup {
  return node.kind === "group"
}

function isMetricsSectionLeaf(
  node: MetricsSectionNode
): node is MetricsSectionLeaf {
  return node.kind === "leaf"
}

function metricsSectionNavLabel(section: MetricsSectionLeaf): string {
  return section.navLabel ?? section.title
}

export type { MetricsSectionGroup, MetricsSectionLeaf, MetricsSectionNode }
export { isMetricsSectionGroup, isMetricsSectionLeaf, metricsSectionNavLabel }
