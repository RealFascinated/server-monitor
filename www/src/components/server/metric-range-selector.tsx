import { useEffect, useId, useState } from "react"
import {
  CalendarDays,
  CalendarRange,
  Check,
  ChevronDown,
  Clock,
  History,
  RefreshCw,
  Timer,
} from "lucide-react"
import type { LucideIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
} from "@/components/ui/select"
import { SimpleTooltip } from "@/components/simple-tooltip"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import {
  METRIC_REFRESH_INTERVAL_OPTIONS,
  getMetricRefreshIntervalOption,
} from "@/lib/metrics/refresh-interval"
import {
  getMetricRangeOption,
  METRIC_RANGE_GROUPS,
  METRIC_RANGE_OPTIONS,
} from "@/lib/metrics/range"
import type { MetricTimeRange } from "@/lib/metrics/range"
import {
  datetimeLocalToEpoch,
  defaultCustomMetricTimeWindow,
  epochToDatetimeLocal,
  formatMetricTimeWindow,
} from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"
import { cn } from "@/lib/utils"

const MOBILE_QUICK_RANGES = [
  "1h",
  "24h",
  "7d",
] as const satisfies readonly MetricTimeRange[]

const MOBILE_QUICK_RANGE_ICONS: Record<
  (typeof MOBILE_QUICK_RANGES)[number],
  LucideIcon
> = {
  "1h": Clock,
  "24h": CalendarDays,
  "7d": CalendarRange,
}

const toolbarIconButtonClassName =
  "flex h-7 w-7 shrink-0 cursor-pointer items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-white/70 hover:text-foreground disabled:cursor-not-allowed disabled:opacity-60 dark:hover:bg-monitor-gray-300/60 dark:hover:text-white"

const toolbarLabeledButtonClassName =
  "flex h-7 shrink-0 items-center gap-1.5 rounded-sm px-2 text-xs font-medium transition-colors"

type MetricRangeSelectorProps = {
  value: MetricTimeWindow
  onChange: (value: MetricTimeWindow) => void
  refreshInterval: MetricRefreshInterval
  onRefreshIntervalChange: (value: MetricRefreshInterval) => void
  onRefresh: () => void
  isRefreshing?: boolean
  className?: string
}

type MetricRangePopoverPanelProps = {
  value: MetricTimeWindow
  fromValue: string
  toValue: string
  customError: string | null
  onFromChange: (value: string) => void
  onToChange: (value: string) => void
  onApplyPreset: (range: MetricTimeRange) => void
  onApplyCustomRange: () => void
}

function MetricRangePopoverPanel({
  value,
  fromValue,
  toValue,
  customError,
  onFromChange,
  onToChange,
  onApplyPreset,
  onApplyCustomRange,
}: MetricRangePopoverPanelProps) {
  const fromId = useId()
  const toId = useId()

  return (
    <div className="relative">
      <section className="border-b border-border/60 p-2 sm:w-54 sm:border-r sm:border-b-0">
        <p className="flex items-center gap-1.5 py-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">
          <History className="size-3.5 shrink-0 opacity-70" aria-hidden />
          Quick ranges
        </p>

        <div role="listbox" aria-label="Quick ranges" className="mt-2.5">
          {METRIC_RANGE_GROUPS.map((group) => {
            const options = METRIC_RANGE_OPTIONS.filter(
              (option) => option.group === group.id
            )

            if (options.length === 0) {
              return null
            }

            return (
              <div
                key={group.id}
                className="[&:not(:first-child)]:mt-2.5 [&:not(:first-child)]:border-t [&:not(:first-child)]:border-border/50 [&:not(:first-child)]:pt-2"
              >
                <p className="pb-0.5 text-[10px] font-semibold tracking-wider text-muted-foreground uppercase">
                  {group.label}
                </p>

                <div className="flex flex-col gap-px">
                  {options.map((option) => {
                    const isActive =
                      value.kind === "preset" && value.range === option.value

                    return (
                      <button
                        key={option.value}
                        type="button"
                        role="option"
                        aria-selected={isActive}
                        onClick={() => onApplyPreset(option.value)}
                        className={cn(
                          "flex w-full cursor-pointer items-center justify-between gap-2 rounded-sm px-2 py-1.5 text-left text-xs transition-colors",
                          isActive
                            ? "bg-foreground/10 font-medium text-foreground"
                            : "text-muted-foreground hover:bg-foreground/5 hover:text-foreground"
                        )}
                      >
                        <span className="min-w-0 truncate">{option.label}</span>
                        <Check
                          aria-hidden={!isActive}
                          className={cn(
                            "size-3.5 shrink-0 text-foreground/70",
                            isActive ? "opacity-100" : "opacity-0"
                          )}
                        />
                      </button>
                    )
                  })}
                </div>
              </div>
            )
          })}
        </div>
      </section>

      <section className="p-3 sm:absolute sm:top-0 sm:right-0 sm:left-54 sm:border-l sm:border-border/60">
        <p className="mb-2.5 flex items-center gap-1.5 text-[11px] font-medium tracking-wide text-muted-foreground uppercase">
          <CalendarRange className="size-3.5 shrink-0 opacity-70" aria-hidden />
          Custom range
        </p>

        <div className="grid gap-2.5">
          <div className="grid gap-1">
            <Label htmlFor={fromId} className="text-xs">
              From
            </Label>
            <Input
              id={fromId}
              type="datetime-local"
              value={fromValue}
              onChange={(event) => onFromChange(event.target.value)}
              className="h-8 bg-background text-xs"
            />
          </div>

          <div className="grid gap-1">
            <Label htmlFor={toId} className="text-xs">
              To
            </Label>
            <Input
              id={toId}
              type="datetime-local"
              value={toValue}
              onChange={(event) => onToChange(event.target.value)}
              className="h-8 bg-background text-xs"
            />
          </div>

          {customError ? (
            <p className="text-xs text-destructive">{customError}</p>
          ) : null}
        </div>

        <Button
          type="button"
          size="sm"
          className="mt-3 h-8 w-full"
          onClick={onApplyCustomRange}
        >
          Apply range
        </Button>
      </section>
    </div>
  )
}

function MetricRefreshButton({
  onRefresh,
  isRefreshing,
}: {
  onRefresh: () => void
  isRefreshing: boolean
}) {
  return (
    <SimpleTooltip content="Refresh metrics">
      <button
        type="button"
        onClick={onRefresh}
        disabled={isRefreshing}
        aria-label="Refresh metrics"
        className={toolbarIconButtonClassName}
      >
        <RefreshCw className={cn("size-3.5", isRefreshing && "animate-spin")} />
      </button>
    </SimpleTooltip>
  )
}

function MetricRangeSelector({
  value,
  onChange,
  refreshInterval,
  onRefreshIntervalChange,
  onRefresh,
  isRefreshing = false,
  className,
}: MetricRangeSelectorProps) {
  const [open, setOpen] = useState(false)
  const activeRefreshInterval = getMetricRefreshIntervalOption(refreshInterval)
  const activeLabel = formatMetricTimeWindow(value)

  const initialCustomWindow =
    value.kind === "custom" ? value : defaultCustomMetricTimeWindow()
  const [fromValue, setFromValue] = useState(
    epochToDatetimeLocal(initialCustomWindow.from)
  )
  const [toValue, setToValue] = useState(
    epochToDatetimeLocal(initialCustomWindow.to)
  )
  const [customError, setCustomError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      return
    }

    const nextWindow =
      value.kind === "custom" ? value : defaultCustomMetricTimeWindow()
    setFromValue(epochToDatetimeLocal(nextWindow.from))
    setToValue(epochToDatetimeLocal(nextWindow.to))
    setCustomError(null)
  }, [open, value])

  function applyPreset(range: MetricTimeRange) {
    onChange({ kind: "preset", range })
    setOpen(false)
  }

  function applyCustomRange() {
    const from = datetimeLocalToEpoch(fromValue)
    const to = datetimeLocalToEpoch(toValue)
    const now = Math.floor(Date.now() / 1000)

    if (!Number.isFinite(from) || !Number.isFinite(to)) {
      setCustomError("Enter valid start and end times.")
      return
    }

    if (from >= to) {
      setCustomError("Start must be before end.")
      return
    }

    if (to > now) {
      setCustomError("End time cannot be in the future.")
      return
    }

    onChange({ kind: "custom", from, to })
    setOpen(false)
  }

  const popoverPanel = (
    <MetricRangePopoverPanel
      value={value}
      fromValue={fromValue}
      toValue={toValue}
      customError={customError}
      onFromChange={(next) => {
        setFromValue(next)
        setCustomError(null)
      }}
      onToChange={(next) => {
        setToValue(next)
        setCustomError(null)
      }}
      onApplyPreset={applyPreset}
      onApplyCustomRange={applyCustomRange}
    />
  )

  const isQuickRangeActive =
    value.kind === "preset" &&
    MOBILE_QUICK_RANGES.includes(
      value.range as (typeof MOBILE_QUICK_RANGES)[number]
    )

  return (
    <div
      className={cn("flex min-w-0 flex-1 items-center gap-0.5", className)}
      role="group"
      aria-label="Time range"
    >
      {MOBILE_QUICK_RANGES.map((range) => {
        const isActive = value.kind === "preset" && value.range === range
        const QuickRangeIcon = MOBILE_QUICK_RANGE_ICONS[range]

        return (
          <button
            key={range}
            type="button"
            aria-label={getMetricRangeOption(range).label}
            aria-pressed={isActive}
            onClick={() => applyPreset(range)}
            className={cn(
              toolbarLabeledButtonClassName,
              "min-w-0 flex-1 justify-center sm:hidden",
              isActive
                ? "bg-white text-monitor shadow-sm dark:bg-monitor-gray-300 dark:text-warning"
                : "text-muted-foreground hover:bg-white/70 hover:text-foreground dark:hover:bg-monitor-gray-300/60 dark:hover:text-white"
            )}
          >
            <QuickRangeIcon className="size-3.5 shrink-0" aria-hidden />
            {getMetricRangeOption(range).shortLabel}
          </button>
        )
      })}

      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button
            type="button"
            aria-label={`Time range: ${activeLabel.label}`}
            className={cn(
              toolbarLabeledButtonClassName,
              "max-w-56 min-w-0 shrink cursor-pointer",
              isQuickRangeActive
                ? "text-muted-foreground hover:bg-white/70 hover:text-foreground sm:bg-white sm:text-monitor sm:shadow-sm dark:hover:bg-monitor-gray-300/60 dark:hover:text-white dark:sm:bg-monitor-gray-300 dark:sm:text-warning"
                : "bg-white text-monitor shadow-sm hover:bg-white/90 dark:bg-monitor-gray-300 dark:text-warning dark:hover:bg-monitor-gray-300/90"
            )}
          >
            <Clock className="size-3.5 shrink-0" />
            <span
              className={cn(
                "truncate max-sm:max-w-16",
                isQuickRangeActive && "hidden sm:inline"
              )}
            >
              {activeLabel.shortLabel}
            </span>
            <ChevronDown className="size-3.5 shrink-0 opacity-60 max-sm:hidden" />
          </button>
        </PopoverTrigger>

        <PopoverContent
          align="end"
          className="w-[min(100vw-2rem,36rem)] bg-popover p-0 backdrop-blur-none"
        >
          {popoverPanel}
        </PopoverContent>
      </Popover>

      <Select
        value={refreshInterval}
        onValueChange={(next) =>
          onRefreshIntervalChange(next as MetricRefreshInterval)
        }
      >
        <SelectTrigger
          size="sm"
          aria-label={`Refresh interval: ${activeRefreshInterval.label}`}
          className={cn(
            toolbarLabeledButtonClassName,
            "hidden w-fit border-0 bg-transparent text-muted-foreground shadow-none hover:bg-white/70 hover:text-foreground focus-visible:ring-1 sm:flex dark:hover:bg-monitor-gray-300/60 dark:hover:text-white [&_svg]:size-3.5"
          )}
        >
          <Timer className="size-3.5 shrink-0 opacity-70" aria-hidden />
          <span>{activeRefreshInterval.shortLabel}</span>
        </SelectTrigger>
        <SelectContent align="end" position="popper" className="min-w-36">
          <SelectGroup>
            <SelectLabel>Auto refresh</SelectLabel>
            {METRIC_REFRESH_INTERVAL_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectGroup>
        </SelectContent>
      </Select>

      <MetricRefreshButton onRefresh={onRefresh} isRefreshing={isRefreshing} />
    </div>
  )
}

export { MetricRangeSelector }
