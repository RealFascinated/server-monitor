import { useEffect, useLayoutEffect, useMemo, useRef } from "react"
import type { RefObject } from "react"
import uPlot from "uplot"
import "uplot/dist/uPlot.min.css"

import {
  bindChartInteractionDismiss,
  createChartTooltipElement,
  createCursorTooltipHandler,
  destroyChartTooltipElement,
} from "@/lib/metrics/chart-tooltip"
import { createThresholdDrawHook } from "@/lib/metrics/chart-thresholds"
import type { ChartThreshold } from "@/lib/metrics/chart-thresholds"
import { stackAlignedData } from "@/lib/metrics/series"
import {
  bindChartZoomNavigate,
  useMetricsChartZoom,
} from "@/lib/metrics/chart-zoom"
import { buildUPlotOptions, getChartColors } from "@/lib/metrics/uplot-theme"
import {
  createChartZoomSyncHook,
  registerMetricsChartSync,
  unregisterMetricsChartSync,
  useMetricsChartSyncKey,
} from "@/lib/metrics/chart-sync"
import { enqueueChartDestroy } from "@/lib/metrics/chart-hydration-queue"
import type { ChartYRange } from "@/lib/metrics/uplot-theme"
import { useTheme } from "@/lib/theme"

export type MetricChartMode = "line" | "stack"

type MetricChartProps = {
  data: uPlot.AlignedData
  labels: string[]
  negated?: boolean[]
  height?: number
  sizeRef?: RefObject<HTMLElement | null>
  valueFormatter?: (value: number) => string
  seriesFormatters?: ((value: number) => string)[]
  yRange?: ChartYRange
  thresholds?: ChartThreshold[]
  mode?: MetricChartMode
}

function MetricChart({
  data,
  labels,
  negated = [],
  height = 260,
  sizeRef,
  valueFormatter,
  seriesFormatters,
  yRange,
  thresholds,
  mode = "line",
}: MetricChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<uPlot | null>(null)
  const valueFormatterRef = useRef(valueFormatter)
  const seriesFormattersRef = useRef(seriesFormatters)
  const dataRef = useRef(data)
  const { resolvedTheme } = useTheme()
  const syncKey = useMetricsChartSyncKey() ?? undefined
  const chartZoom = useMetricsChartZoom()
  const chartZoomRef = useRef(chartZoom)
  const yMax = yRange?.max ?? null
  const labelsKey = labels.join("\0")
  const negatedKey = negated.map(String).join("\0")
  const thresholdsKey =
    thresholds?.map((entry) => `${entry.level}:${entry.value}`).join("|") ?? ""
  const stacked = mode === "stack"
  const bidirectional = negated.some(Boolean)

  const prepared = useMemo(() => {
    if (!stacked) {
      return { data, bands: undefined }
    }

    const result = stackAlignedData(data)
    return { data: result.data, bands: result.bands }
  }, [data, stacked])

  valueFormatterRef.current = valueFormatter
  seriesFormattersRef.current = seriesFormatters
  dataRef.current = data
  chartZoomRef.current = chartZoom

  useEffect(() => {
    const container = containerRef.current
    if (!container) {
      return
    }

    let disposed = false
    let chart: uPlot | null = null
    let resizeObserver: ResizeObserver | null = null
    let unbindInteractionDismiss: (() => void) | null = null
    let unbindZoomNavigate: (() => void) | null = null
    let tooltip: HTMLDivElement | null = null

    const frame = requestAnimationFrame(() => {
      if (disposed) {
        return
      }

      tooltip = createChartTooltipElement(resolvedTheme)

      const getSizeElement = () => sizeRef?.current ?? containerRef.current

      const getChartSize = () => {
        const element = getSizeElement()
        const width = Math.max(element?.clientWidth ?? 1, 1)
        const chartHeight = Math.max(element?.clientHeight ?? height, height)
        return { width, height: chartHeight }
      }

      const { width: initialWidth, height: initialHeight } = getChartSize()

      const options = buildUPlotOptions({
        theme: resolvedTheme,
        labels,
        height: initialHeight,
        valueFormatter: (value) =>
          valueFormatterRef.current?.(value) ?? String(value),
        yRange,
        stacked,
        bands: prepared.bands,
        bidirectional,
        negated,
      })

      const colors = getChartColors(resolvedTheme)
      const formatSeriesValue = (value: number, seriesIndex: number) => {
        const display =
          seriesIndex >= 0 && negated[seriesIndex] ? Math.abs(value) : value
        const formatter =
          (seriesIndex >= 0
            ? seriesFormattersRef.current?.[seriesIndex]
            : undefined) ?? valueFormatterRef.current
        return formatter?.(display) ?? String(display)
      }
      const hooks: uPlot.Hooks.Arrays = {
        setCursor: [
          createCursorTooltipHandler({
            tooltip,
            labels,
            colors,
            getData: () => dataRef.current,
            formatValue: formatSeriesValue,
            theme: resolvedTheme,
            stacked,
          }),
        ],
        ...(syncKey ? { setScale: [createChartZoomSyncHook(syncKey)] } : {}),
      }

      if (thresholds && thresholds.length > 0) {
        hooks.drawAxes = [createThresholdDrawHook(thresholds, resolvedTheme)]
      }

      options.hooks = hooks

      chart = new uPlot(
        { ...options, width: initialWidth },
        prepared.data,
        container
      )
      chartRef.current = chart

      if (syncKey) {
        registerMetricsChartSync(syncKey, chart)
      }

      resizeObserver = new ResizeObserver(() => {
        const { width, height: chartHeight } = getChartSize()
        if (width > 0 && chartHeight > 0) {
          chart?.setSize({ width, height: chartHeight })
        }
      })
      const sizeElement = getSizeElement()
      if (sizeElement) {
        resizeObserver.observe(sizeElement)
      }
      unbindInteractionDismiss = bindChartInteractionDismiss(chart, tooltip)

      const zoom = chartZoomRef.current
      if (zoom) {
        unbindZoomNavigate = bindChartZoomNavigate(chart, zoom.getZoomContext)
      }
    })

    return () => {
      disposed = true
      cancelAnimationFrame(frame)
      unbindInteractionDismiss?.()
      unbindZoomNavigate?.()
      resizeObserver?.disconnect()
      if (tooltip) {
        destroyChartTooltipElement(tooltip)
      }
      if (chart) {
        if (syncKey) {
          unregisterMetricsChartSync(syncKey, chart)
        }
        enqueueChartDestroy(chart)
      }
      chartRef.current = null
    }
  }, [
    resolvedTheme,
    labelsKey,
    height,
    yMax,
    thresholdsKey,
    stacked,
    bidirectional,
    negatedKey,
    prepared.bands,
    thresholds,
    syncKey,
  ])

  useLayoutEffect(() => {
    chartRef.current?.setData(prepared.data)
  }, [prepared.data])

  return (
    <div className="absolute inset-0 overflow-visible">
      <div ref={containerRef} className="h-full w-full overflow-visible" />
    </div>
  )
}

export { MetricChart }
