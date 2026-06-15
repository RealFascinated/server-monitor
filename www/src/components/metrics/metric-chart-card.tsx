import { memo, useMemo, useRef, useState } from "react"
import type { RefObject } from "react"
import { Maximize2 } from "lucide-react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { MetricChart } from "@/components/metrics/metric-chart"
import { useChartHydration } from "@/hooks/use-chart-hydration"
import { getChartColor } from "@/lib/metrics/chart-colors"
import {
  buildMultiSeriesData,
  formatSeriesRangeTooltip,
  getLatestValue,
  hasSeriesData,
  sortSeriesForStack,
} from "@/lib/metrics/series"
import type { ChartSeries } from "@/lib/metrics/series"
import type { MetricsTimeGrid } from "@/lib/metrics/timestamps"
import type { MetricChartMode } from "@/components/metrics/metric-chart"
import type { ChartThreshold } from "@/lib/metrics/chart-thresholds"
import type { ChartYRange } from "@/lib/metrics/uplot-theme"
import { cn } from "@/lib/utils"
import { useTheme } from "@/lib/theme"

type MetricChartCardProps = {
  timeGrid: MetricsTimeGrid
  title: string
  description?: string
  series: ChartSeries[]
  height?: number
  valueFormatter?: (value: number) => string
  seriesFormatters?: ((value: number) => string)[]
  yRange?: ChartYRange
  thresholds?: ChartThreshold[]
  showCurrentValues?: boolean
  mode?: MetricChartMode
}

const FULLSCREEN_CHART_MIN_HEIGHT = 480

type MetricChartPanelProps = {
  containerRef: RefObject<HTMLDivElement | null>
  height: number
  built: ReturnType<typeof buildMultiSeriesData> | null
  valueFormatter?: (value: number) => string
  seriesFormatters?: ((value: number) => string)[]
  yRange?: ChartYRange
  thresholds?: ChartThreshold[]
  mode?: MetricChartMode
  className?: string
}

function MetricChartPanel({
  containerRef,
  height,
  built,
  valueFormatter,
  seriesFormatters,
  yRange,
  thresholds,
  mode,
  className,
}: MetricChartPanelProps) {
  return (
    <div
      ref={containerRef}
      className={cn("relative min-h-0 flex-1", className)}
      style={{ minHeight: height }}
    >
      {built ? (
        <MetricChart
          sizeRef={containerRef}
          data={built.data}
          labels={built.labels}
          negated={built.negated}
          height={height}
          valueFormatter={valueFormatter}
          seriesFormatters={seriesFormatters}
          yRange={yRange}
          thresholds={thresholds}
          mode={mode}
        />
      ) : null}
    </div>
  )
}

function MetricChartCard({
  timeGrid,
  title,
  description,
  series,
  height,
  valueFormatter,
  seriesFormatters,
  yRange,
  thresholds,
  showCurrentValues,
  mode,
}: MetricChartCardProps) {
  const chartHeight = height ?? 260
  const [fullscreenOpen, setFullscreenOpen] = useState(false)
  const { inView, containerRef } = useChartHydration()
  const fullscreenContainerRef = useRef<HTMLDivElement>(null)
  const chartReady = inView || fullscreenOpen
  const displaySeries = useMemo(
    () => (mode === "stack" ? sortSeriesForStack(series) : series),
    [mode, series]
  )
  const built = useMemo(() => {
    if (!chartReady || !hasSeriesData(displaySeries)) {
      return null
    }

    return buildMultiSeriesData(
      timeGrid.gridTimestamps,
      timeGrid.sourceTimestamps,
      displaySeries
    )
  }, [chartReady, timeGrid, displaySeries])
  const { resolvedTheme } = useTheme()
  const shouldShowCurrentValues = showCurrentValues ?? displaySeries.length <= 4

  const formatSeriesValue = (index: number, value: number) => {
    const formatter = seriesFormatters?.[index] ?? valueFormatter
    return formatter ? formatter(value) : String(value)
  }

  if (!hasSeriesData(series)) {
    return null
  }

  const titleNode = (
    <CardTitle
      className={cn(
        "text-sm font-bold text-foreground",
        description && "cursor-help"
      )}
    >
      {title}
    </CardTitle>
  )

  const currentValuesNode = shouldShowCurrentValues ? (
    <div className="flex flex-wrap justify-end gap-x-3 gap-y-1.5">
      {displaySeries.map((entry, index) => {
        const value = getLatestValue(entry.values)
        if (value == null) {
          return null
        }

        const formatted = formatSeriesValue(index, value)

        const valueTooltip =
          formatSeriesRangeTooltip(entry.values, timeGrid, (nextValue) =>
            formatSeriesValue(index, nextValue)
          ) ?? formatted

        return (
          <SimpleTooltip key={entry.label} content={valueTooltip}>
            <span className="inline-flex cursor-help items-center gap-1.5 text-xs">
              <span
                aria-hidden
                className="size-2 shrink-0 rounded-full"
                style={{
                  backgroundColor: getChartColor(index, resolvedTheme),
                }}
              />
              {displaySeries.length > 1 ? (
                <span className="text-muted-foreground">{entry.label}</span>
              ) : null}
              <span className="font-mono text-sm font-medium text-foreground tabular-nums">
                {formatted}
              </span>
            </span>
          </SimpleTooltip>
        )
      })}
    </div>
  ) : null

  const expandButton = (
    <SimpleTooltip content="Expand chart">
      <Button
        type="button"
        variant="ghost"
        size="icon-xs"
        className="shrink-0 text-muted-foreground"
        onClick={() => setFullscreenOpen(true)}
        aria-label="Expand chart"
      >
        <Maximize2 />
      </Button>
    </SimpleTooltip>
  )

  return (
    <Card className="flex h-full min-h-0 flex-col gap-0 overflow-hidden py-0 dark:border-monitor-gray-300">
      <CardHeader className="shrink-0 gap-2 border-b border-border bg-neutral-100/90 px-4 py-3 dark:border-monitor-gray-300 dark:bg-monitor-gray-200/60">
        <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
          <div className="flex min-w-0 items-center gap-0.5">
            {description ? (
              <SimpleTooltip content={description}>{titleNode}</SimpleTooltip>
            ) : (
              titleNode
            )}
            {expandButton}
          </div>
          {currentValuesNode}
        </div>
      </CardHeader>
      <CardContent className="flex min-h-0 flex-1 flex-col px-3 pt-2 pb-3">
        <MetricChartPanel
          containerRef={containerRef}
          height={chartHeight}
          built={built}
          valueFormatter={valueFormatter}
          seriesFormatters={seriesFormatters}
          yRange={yRange}
          thresholds={thresholds}
          mode={mode}
        />
      </CardContent>

      <Dialog open={fullscreenOpen} onOpenChange={setFullscreenOpen}>
        <DialogContent
          showCloseButton
          className="flex h-[min(90vh,calc(100vh-2rem))] w-[min(96vw,calc(100vw-2rem))] max-w-none animate-none! flex-col gap-0 overflow-hidden p-0 data-closed:animate-none! data-open:animate-none! sm:max-w-none"
        >
          <DialogHeader className="shrink-0 gap-2 border-b border-border bg-neutral-100/90 px-4 py-3 dark:border-monitor-gray-300 dark:bg-monitor-gray-200/60">
            <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2 pr-8">
              <div className="min-w-0">
                {description ? (
                  <SimpleTooltip content={description}>
                    <DialogTitle className="cursor-help text-sm font-bold text-foreground">
                      {title}
                    </DialogTitle>
                  </SimpleTooltip>
                ) : (
                  <DialogTitle className="text-sm font-bold text-foreground">
                    {title}
                  </DialogTitle>
                )}
              </div>
              {currentValuesNode}
            </div>
          </DialogHeader>
          <div className="flex min-h-0 flex-1 flex-col px-3 pt-2 pb-3">
            <MetricChartPanel
              containerRef={fullscreenContainerRef}
              height={FULLSCREEN_CHART_MIN_HEIGHT}
              built={built}
              valueFormatter={valueFormatter}
              seriesFormatters={seriesFormatters}
              yRange={yRange}
              thresholds={thresholds}
              mode={mode}
            />
          </div>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

const MemoizedMetricChartCard = memo(MetricChartCard)

export { MemoizedMetricChartCard as MetricChartCard }
