import { ArrowDownUp } from "lucide-react"

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  FLEET_FILTER_LABELS,
  FLEET_FILTERS,
  SERVER_SORT_LABELS,
  SERVER_SORTS,
} from "@/lib/servers/fleet-filter"
import type { FleetFilter, ServerSort } from "@/lib/servers/fleet-filter"
import { cn } from "@/lib/utils"

type ServersFilterBarProps = {
  counts: Record<FleetFilter, number>
  filter: FleetFilter
  onFilterChange: (filter: FleetFilter) => void
  sort: ServerSort
  onSortChange: (sort: ServerSort) => void
  sortDisabled?: boolean
}

function ServersFilterBar({
  counts,
  filter,
  onFilterChange,
  sort,
  onSortChange,
  sortDisabled = false,
}: ServersFilterBarProps) {
  // Always show the active chip so it can be cleared, even if its count drops to 0.
  const visibleFilters = FLEET_FILTERS.filter(
    (entry) =>
      entry === "all" ||
      entry === "online" ||
      entry === filter ||
      counts[entry] > 0
  )

  return (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <div
        role="group"
        aria-label="Filter servers by status"
        className="flex flex-wrap items-center gap-1.5"
      >
        {visibleFilters.map((entry) => {
          const active = entry === filter

          return (
            <button
              key={entry}
              type="button"
              aria-pressed={active}
              onClick={() => onFilterChange(entry)}
              className={cn(
                "inline-flex h-7 items-center gap-1.5 rounded-full border px-3 text-xs font-medium transition-colors focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none",
                active
                  ? "border-primary bg-primary/10 text-brand"
                  : "border-border bg-card text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              {FLEET_FILTER_LABELS[entry]}
              <span
                className={cn(
                  "tabular-nums",
                  active ? "text-brand" : "text-muted-foreground"
                )}
              >
                {counts[entry]}
              </span>
            </button>
          )
        })}
      </div>

      <Select
        value={sort}
        onValueChange={(value) => onSortChange(value as ServerSort)}
        disabled={sortDisabled}
      >
        <SelectTrigger size="sm" aria-label="Sort servers" className="min-w-44">
          <ArrowDownUp aria-hidden className="size-3.5 text-muted-foreground" />
          <SelectValue />
        </SelectTrigger>
        <SelectContent align="end">
          {SERVER_SORTS.map((entry) => (
            <SelectItem key={entry} value={entry}>
              {SERVER_SORT_LABELS[entry]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}

export { ServersFilterBar }
