import type { LucideIcon } from "lucide-react"
import type { ReactNode } from "react"

import { metricSectionId } from "@/lib/metrics/sections/id"
import type {
  MetricsSectionLeaf,
  MetricsSectionNode,
} from "@/lib/metrics/sections/types"

type LeafInput = {
  id?: string
  title: string
  navLabel?: string
  navPercent?: number | null
  navPercentTooltip?: string
  icon: LucideIcon
  description?: string
  render: () => ReactNode
}

type GroupInput = {
  id: string
  title: string
  icon: LucideIcon
  showChildCount?: boolean
}

function createLeaf(input: LeafInput): MetricsSectionLeaf {
  return {
    kind: "leaf",
    id: input.id ?? metricSectionId(input.title),
    title: input.title,
    navLabel: input.navLabel,
    navPercent: input.navPercent,
    navPercentTooltip: input.navPercentTooltip,
    icon: input.icon,
    description: input.description,
    render: input.render,
  }
}

class MetricsSectionGroupBuilder {
  private nodes: MetricsSectionNode[] = []

  leaf(input: LeafInput) {
    this.nodes.push(createLeaf(input))
  }

  group(
    input: GroupInput,
    buildChildren: (group: MetricsSectionGroupBuilder) => void
  ) {
    const groupBuilder = new MetricsSectionGroupBuilder()
    buildChildren(groupBuilder)
    const children = groupBuilder.build()

    if (children.length === 0) {
      return
    }

    this.nodes.push({
      kind: "group",
      id: input.id,
      title: input.title,
      icon: input.icon,
      showChildCount: input.showChildCount,
      children,
    })
  }

  build(): MetricsSectionNode[] {
    return this.nodes
  }
}

class MetricsSectionBuilder {
  private nodes: MetricsSectionNode[] = []

  leaf(input: LeafInput) {
    this.nodes.push(createLeaf(input))
  }

  group(
    input: GroupInput,
    buildChildren: (group: MetricsSectionGroupBuilder) => void
  ) {
    const groupBuilder = new MetricsSectionGroupBuilder()
    buildChildren(groupBuilder)
    const children = groupBuilder.build()

    if (children.length === 0) {
      return
    }

    this.nodes.push({
      kind: "group",
      id: input.id,
      title: input.title,
      icon: input.icon,
      showChildCount: input.showChildCount,
      children,
    })
  }

  build(): MetricsSectionNode[] {
    return this.nodes
  }
}

function createMetricsSectionBuilder() {
  return new MetricsSectionBuilder()
}

export { createMetricsSectionBuilder }
export type { LeafInput, GroupInput }
