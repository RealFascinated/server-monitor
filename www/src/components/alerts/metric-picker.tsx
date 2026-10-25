import { Check, ChevronsUpDown, Search } from "lucide-react"
import { useMemo, useState } from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import type { AlertMetric } from "@/lib/api/user/alerts"
import { cn } from "@/lib/utils"

type MetricPickerProps = {
  metrics: AlertMetric[]
  value: string
  invalid?: boolean
  onChange: (metric: AlertMetric) => void
}

function matches(metric: AlertMetric, query: string): boolean {
  const haystack =
    `${metric.label} ${metric.group} ${metric.metric}`.toLowerCase()
  return query
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((part) => haystack.includes(part))
}

/** Searchable, grouped metric chooser; the catalog has dozens of entries so a plain select doesn't scale. */
function MetricPicker({
  metrics,
  value,
  invalid,
  onChange,
}: MetricPickerProps) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")

  const selected = metrics.find((metric) => metric.metric === value)
  const groups = useMemo(() => {
    const result = new Map<string, AlertMetric[]>()
    for (const metric of metrics.filter((item) => matches(item, query))) {
      result.set(metric.group, [...(result.get(metric.group) ?? []), metric])
    }
    return [...result.entries()]
  }, [metrics, query])

  return (
    <Popover
      open={open}
      onOpenChange={(next) => {
        setOpen(next)
        if (next) setQuery("")
      }}
    >
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          aria-invalid={invalid ? true : undefined}
          className="h-9 w-full justify-between font-normal"
        >
          <span
            className={cn("truncate", !selected && "text-muted-foreground")}
          >
            {selected?.label ?? "Search metrics…"}
          </span>
          <ChevronsUpDown aria-hidden className="size-4 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) min-w-72 p-0"
      >
        <div className="flex items-center gap-2 border-b border-border px-3">
          <Search aria-hidden className="size-4 text-muted-foreground" />
          <Input
            autoFocus
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search CPU, disk, temperature…"
            className="h-10 border-0 px-0 shadow-none focus-visible:ring-0"
          />
        </div>
        <div className="max-h-72 overflow-y-auto p-1" role="listbox">
          {groups.length === 0 ? (
            <p className="px-3 py-6 text-center text-sm text-muted-foreground">
              No metric matches &ldquo;{query}&rdquo;.
            </p>
          ) : (
            groups.map(([group, items]) => (
              <div key={group} className="py-1">
                <p className="px-2 py-1 text-xs font-medium tracking-wide text-muted-foreground uppercase">
                  {group}
                </p>
                {items.map((metric) => (
                  <button
                    key={metric.metric}
                    type="button"
                    role="option"
                    aria-selected={metric.metric === value}
                    onClick={() => {
                      onChange(metric)
                      setOpen(false)
                    }}
                    className="flex w-full items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-left text-sm hover:bg-muted focus-visible:bg-muted focus-visible:outline-none"
                  >
                    {metric.label}
                    {metric.metric === value ? (
                      <Check aria-hidden className="size-4 text-brand" />
                    ) : null}
                  </button>
                ))}
              </div>
            ))
          )}
        </div>
      </PopoverContent>
    </Popover>
  )
}

export { MetricPicker }
