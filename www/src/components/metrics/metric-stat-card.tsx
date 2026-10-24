import { Check } from "lucide-react"

import { CountUp } from "@/components/count-up"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Sparkline } from "@/components/sparkline"
import { UsageMeter } from "@/components/server/usage-bar"
import { Card, CardContent } from "@/components/ui/card"
import { percentTrendColorClass } from "@/lib/metrics/percent-level"
import { sparklineStats } from "@/lib/metrics/sparkline"
import type { SparklineValues } from "@/lib/metrics/sparkline"
import { cn } from "@/lib/utils"

type MetricStatCardProps = {
  title: string
  value: number
  formatValue: (value: number) => string
  detail?: string
  detailTooltip?: string
  valueClassName?: string
  /** Unit or label set in the body font beside the number, e.g. "online". */
  valueSuffix?: string
  animate?: boolean
  /** 0 to 100 fill for a usage meter under the value. */
  progress?: number | null
  /** Recent history drawn beside the value. */
  trend?: SparklineValues
  trendLabel?: string
  /** Makes the whole card a toggle button (e.g. a dashboard filter). */
  onSelect?: () => void
  selected?: boolean
  selectLabel?: string
}

function MetricStatCard({
  title,
  value,
  formatValue,
  detail,
  detailTooltip,
  valueClassName,
  valueSuffix,
  animate = true,
  progress,
  trend,
  trendLabel,
  onSelect,
  selected = false,
  selectLabel,
}: MetricStatCardProps) {
  const valueClass = cn(
    "font-mono text-3xl font-semibold tracking-tight text-foreground tabular-nums",
    valueClassName
  )
  const trendStats = trend ? sparklineStats(trend) : null

  const detailNode = detail ? (
    detailTooltip ? (
      <SimpleTooltip content={detailTooltip}>
        <p className="tooltip-hint truncate text-sm text-muted-foreground">
          {detail}
        </p>
      </SimpleTooltip>
    ) : (
      <p className="truncate text-sm text-muted-foreground">{detail}</p>
    )
  ) : null

  const card = (
    <Card
      className={cn(
        "min-h-34 gap-0 overflow-hidden py-0 transition-colors",
        onSelect && "h-full",
        onSelect && "group-hover/stat:bg-card-hover",
        selected && "border-primary ring-1 ring-primary"
      )}
    >
      <CardContent className="flex flex-col gap-2 px-4 py-4">
        <div className="flex items-center justify-between gap-2">
          <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
            {title}
          </p>
          {selected ? (
            <Check aria-hidden className="size-3.5 text-brand" />
          ) : null}
        </div>
        <div className="flex flex-wrap items-end justify-between gap-x-3 gap-y-1">
          <div className="flex shrink-0 items-baseline gap-1.5">
            {animate ? (
              <CountUp
                value={value}
                format={formatValue}
                className={valueClass}
              />
            ) : (
              <span className={valueClass}>{formatValue(value)}</span>
            )}
            {valueSuffix ? (
              <span className="font-medium text-base text-muted-foreground">
                {valueSuffix}
              </span>
            ) : null}
          </div>
          {trend && trendStats ? (
            <Sparkline
              values={trend}
              label={trendLabel ?? `${title} trend`}
              width={96}
              height={32}
              className={percentTrendColorClass(trendStats.latest)}
            />
          ) : null}
        </div>
        {progress !== undefined ? (
          <UsageMeter value={progress} className="flex-none" />
        ) : null}
        {detailNode}
      </CardContent>
    </Card>
  )

  if (!onSelect) {
    return card
  }

  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      aria-label={selectLabel}
      className="group/stat block w-full rounded-lg text-left focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none"
    >
      {card}
    </button>
  )
}

export { MetricStatCard }
