import { isMetricsSectionGroup } from "@/lib/metrics/sections/types"
import type {
  MetricsSectionLeaf,
  MetricsSectionNode,
} from "@/lib/metrics/sections/types"

function flattenMetricSectionLeaves(
  nodes: MetricsSectionNode[]
): MetricsSectionLeaf[] {
  const leaves: MetricsSectionLeaf[] = []

  for (const node of nodes) {
    if (isMetricsSectionGroup(node)) {
      leaves.push(...flattenMetricSectionLeaves(node.children))
      continue
    }

    leaves.push(node)
  }

  return leaves
}

function collectGroupIds(nodes: MetricsSectionNode[]): string[] {
  const groupIds: string[] = []

  for (const node of nodes) {
    if (!isMetricsSectionGroup(node)) {
      continue
    }

    groupIds.push(node.id, ...collectGroupIds(node.children))
  }

  return groupIds
}

function metricsSectionIdsKey(nodes: MetricsSectionNode[]): string {
  return flattenMetricSectionLeaves(nodes)
    .map((section) => section.id)
    .join("|")
}

export { collectGroupIds, flattenMetricSectionLeaves, metricsSectionIdsKey }
