import { ChevronRight } from "lucide-react"
import { useEffect, useMemo, useState } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { formatPercent } from "@/lib/formatter"
import { percentLevelColorClass } from "@/lib/metrics/percent-level"
import { CollapsiblePanel } from "@/components/collapsible-panel"
import { cn } from "@/lib/utils"
import {
  collectGroupIds,
  flattenMetricSectionLeaves,
  metricsSectionIdsKey,
} from "@/lib/metrics/sections/flatten"
import {
  isMetricsSectionGroup,
  metricsSectionNavLabel,
} from "@/lib/metrics/sections/types"
import type {
  MetricsSectionGroup,
  MetricsSectionLeaf,
  MetricsSectionNode,
} from "@/lib/metrics/sections/types"

type MetricsSectionNavProps = {
  sections: MetricsSectionNode[]
  activeId: string
  onScrollToSection: (id: string) => void
}

function groupContainsActiveDescendant(
  group: MetricsSectionGroup,
  activeId: string
): boolean {
  return group.children.some((child) => {
    if (child.id === activeId) {
      return true
    }

    return (
      isMetricsSectionGroup(child) &&
      groupContainsActiveDescendant(child, activeId)
    )
  })
}

function findAncestorGroupIds(
  nodes: MetricsSectionNode[],
  leafId: string
): string[] {
  for (const node of nodes) {
    if (!isMetricsSectionGroup(node)) {
      if (node.id === leafId) {
        return []
      }

      continue
    }

    if (!groupContainsActiveDescendant(node, leafId)) {
      continue
    }

    return [node.id, ...findAncestorGroupIds(node.children, leafId)]
  }

  return []
}

const navItemClassName = (isActive: boolean, depth: number) =>
  cn(
    "flex w-full items-center gap-1 rounded-sm py-1 text-left text-xs leading-snug transition-colors",
    depth > 0 ? "pr-2" : "px-2",
    isActive
      ? "bg-neutral-200/90 font-medium text-foreground dark:bg-monitor-gray-200 dark:text-warning"
      : "text-muted-foreground hover:bg-muted/80 hover:text-foreground"
  )

function NavLeafButton({
  section,
  isActive,
  onScrollToSection,
  depth,
}: {
  section: MetricsSectionLeaf
  isActive: boolean
  onScrollToSection: (id: string) => void
  depth: number
}) {
  const percent = section.navPercent != null && (
    <span
      className={cn(
        "ml-auto shrink-0 text-[10px] tabular-nums",
        percentLevelColorClass(section.navPercent)
      )}
    >
      {formatPercent(section.navPercent)}
    </span>
  )

  return (
    <button
      type="button"
      onClick={() => onScrollToSection(section.id)}
      className={navItemClassName(isActive, depth)}
      style={depth > 0 ? { paddingLeft: `${8 + depth * 16}px` } : undefined}
    >
      <span className="min-w-0 flex-1 truncate">
        {metricsSectionNavLabel(section)}
      </span>
      {section.navPercent != null && section.navPercentTooltip ? (
        <SimpleTooltip content={section.navPercentTooltip}>
          <span className="cursor-help">{percent}</span>
        </SimpleTooltip>
      ) : (
        percent
      )}
    </button>
  )
}

function NavGroup({
  group,
  activeId,
  depth,
  expandedGroups,
  onToggle,
  onScrollToSection,
}: {
  group: MetricsSectionGroup
  activeId: string
  depth: number
  expandedGroups: Set<string>
  onToggle: (groupId: string) => void
  onScrollToSection: (id: string) => void
}) {
  const expanded = expandedGroups.has(group.id)
  const hasActiveChild = groupContainsActiveDescendant(group, activeId)

  return (
    <div className="flex flex-col">
      <button
        type="button"
        onClick={() => onToggle(group.id)}
        aria-expanded={expanded}
        className={cn(
          "flex w-full items-center gap-1 rounded-sm py-1 text-left text-xs leading-snug font-medium transition-colors",
          depth > 0 ? "pr-2" : "px-2",
          hasActiveChild
            ? "text-foreground"
            : "text-muted-foreground hover:bg-muted/80 hover:text-foreground"
        )}
        style={depth > 0 ? { paddingLeft: `${8 + depth * 16}px` } : undefined}
      >
        <ChevronRight
          aria-hidden
          className={cn(
            "size-3 shrink-0 opacity-60 transition-transform duration-150",
            expanded && "rotate-90"
          )}
        />
        <span className="truncate">{group.title}</span>
        {group.showChildCount !== false ? (
          <span className="ml-auto shrink-0 text-[10px] text-muted-foreground tabular-nums">
            {group.children.length}
          </span>
        ) : null}
      </button>

      <CollapsiblePanel open={expanded} className="flex flex-col gap-px pb-0.5">
        {group.children.map((child) => (
          <NavNode
            key={child.id}
            node={child}
            activeId={activeId}
            depth={depth + 1}
            expandedGroups={expandedGroups}
            onToggle={onToggle}
            onScrollToSection={onScrollToSection}
          />
        ))}
      </CollapsiblePanel>
    </div>
  )
}

function NavNode({
  node,
  activeId,
  depth,
  expandedGroups,
  onToggle,
  onScrollToSection,
}: {
  node: MetricsSectionNode
  activeId: string
  depth: number
  expandedGroups: Set<string>
  onToggle: (groupId: string) => void
  onScrollToSection: (id: string) => void
}) {
  if (isMetricsSectionGroup(node)) {
    return (
      <NavGroup
        group={node}
        activeId={activeId}
        depth={depth}
        expandedGroups={expandedGroups}
        onToggle={onToggle}
        onScrollToSection={onScrollToSection}
      />
    )
  }

  return (
    <NavLeafButton
      section={node}
      isActive={node.id === activeId}
      onScrollToSection={onScrollToSection}
      depth={depth}
    />
  )
}

function MetricsSectionNav({
  sections,
  activeId,
  onScrollToSection,
}: MetricsSectionNavProps) {
  const sectionIdsKey = metricsSectionIdsKey(sections)
  const groupIds = useMemo(
    () => collectGroupIds(sections),
    [sectionIdsKey, sections]
  )

  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(
    () => new Set(groupIds)
  )

  useEffect(() => {
    setExpandedGroups((current) => {
      const next = new Set(current)

      for (const groupId of groupIds) {
        next.add(groupId)
      }

      return next
    })
  }, [groupIds])

  useEffect(() => {
    const ancestorGroupIds = findAncestorGroupIds(sections, activeId)

    if (ancestorGroupIds.length === 0) {
      return
    }

    setExpandedGroups((current) => {
      const next = new Set(current)
      let changed = false

      for (const groupId of ancestorGroupIds) {
        if (!next.has(groupId)) {
          next.add(groupId)
          changed = true
        }
      }

      return changed ? next : current
    })
  }, [activeId, sections])

  function toggleGroup(groupId: string) {
    setExpandedGroups((current) => {
      const next = new Set(current)

      if (next.has(groupId)) {
        next.delete(groupId)
      } else {
        next.add(groupId)
      }

      return next
    })
  }

  const leafCount = useMemo(
    () => flattenMetricSectionLeaves(sections).length,
    [sectionIdsKey, sections]
  )

  if (leafCount <= 1) {
    return null
  }

  return (
    <nav
      className="hidden w-48 shrink-0 xl:block 2xl:w-52"
      aria-label="Metric sections"
    >
      <div className="sticky top-[calc(var(--metrics-header-offset)+1rem)] z-20 flex max-h-[calc(100svh-var(--metrics-header-offset)-2rem)] flex-col overflow-hidden rounded-sm border border-sidebar-border bg-sidebar shadow-sm">
        <div className="border-b border-sidebar-border px-3 py-2">
          <p className="text-[10px] font-semibold tracking-wide text-muted-foreground uppercase">
            Sections
          </p>
        </div>

        <div className="flex flex-col gap-px overflow-y-auto p-1.5">
          {sections.map((node) => (
            <NavNode
              key={node.id}
              node={node}
              activeId={activeId}
              depth={0}
              expandedGroups={expandedGroups}
              onToggle={toggleGroup}
              onScrollToSection={onScrollToSection}
            />
          ))}
        </div>
      </div>
    </nav>
  )
}

export { MetricsSectionNav }
