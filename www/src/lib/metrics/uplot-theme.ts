import { formatChartAxisTicks } from "@/lib/formatter"
import { getChartColor, getChartColors } from "@/lib/metrics/chart-colors"
import type { ResolvedTheme } from "@/lib/theme/context"
import uPlot from "uplot"

export { getChartColor, getChartColors }

const AXIS_FONT =
  '12px "Inter Variable", Inter, ui-sans-serif, system-ui, sans-serif'
const X_AXIS_HEIGHT = 24

let textMeasureCtx: CanvasRenderingContext2D | null = null

function measureAxisWidth(values: string[] | null): number {
  const minWidth = 48
  if (!values || values.length === 0) {
    return minWidth
  }

  if (!textMeasureCtx) {
    textMeasureCtx = document.createElement("canvas").getContext("2d")
  }
  if (!textMeasureCtx) {
    return minWidth
  }

  textMeasureCtx.font = AXIS_FONT
  let max = 0
  for (const value of values) {
    max = Math.max(max, textMeasureCtx.measureText(value).width)
  }

  return Math.max(minWidth, Math.ceil(max) + 12)
}

export type ChartYRange = {
  min?: number
  max?: number
}

function buildYScale(yRange?: ChartYRange, bidirectional = false): uPlot.Scale {
  if (bidirectional) {
    return {
      range: (_self, dataMin, dataMax) => {
        const extent = Math.max(Math.abs(dataMin), Math.abs(dataMax), 1)
        const [, padded] = uPlot.rangeNum(-extent, extent, 0.1, true)
        const max = padded ?? extent
        return [-max, max]
      },
    }
  }

  const yMin = yRange?.min ?? 0

  if (yRange?.max != null) {
    return {
      auto: false,
      range: [yMin, yRange.max],
    }
  }

  return {
    range: (_self, dataMin, dataMax) => {
      const [min, max] = uPlot.rangeNum(dataMin, dataMax, 0.1, true)
      return [Math.min(yMin, min ?? yMin), Math.max(yMin, max ?? yMin + 1)]
    },
  }
}

function withAlpha(color: string, alpha: number): string {
  if (!color.startsWith("#") || (color.length !== 7 && color.length !== 4)) {
    return color
  }

  if (color.length === 4) {
    const r = color[1]
    const g = color[2]
    const b = color[3]
    return `rgba(${Number.parseInt(`${r}${r}`, 16)}, ${Number.parseInt(`${g}${g}`, 16)}, ${Number.parseInt(`${b}${b}`, 16)}, ${alpha})`
  }

  const r = Number.parseInt(color.slice(1, 3), 16)
  const g = Number.parseInt(color.slice(3, 5), 16)
  const b = Number.parseInt(color.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

type BuildUPlotOptionsParams = {
  theme: ResolvedTheme
  labels: string[]
  height: number
  valueFormatter?: (value: number) => string
  yRange?: ChartYRange
  stacked?: boolean
  bands?: uPlot.Band[]
  bidirectional?: boolean
  negated?: boolean[]
}

export function buildUPlotOptions({
  theme,
  labels,
  height,
  valueFormatter,
  yRange,
  stacked = false,
  bands,
  bidirectional = false,
  negated = [],
}: BuildUPlotOptionsParams): uPlot.Options {
  const colors = getChartColors(theme)
  const isDark = theme === "dark"
  const gridColor = isDark ? "#282828" : "#e5e5e5"
  const axisColor = isDark ? "#737373" : "#a3a3a3"
  const formatAxisValue = (value: number) => {
    const display = bidirectional ? Math.abs(value) : value
    return valueFormatter ? valueFormatter(display) : String(display)
  }
  return {
    width: 0,
    height,
    series: [
      { show: false },
      ...labels.map((label, index) => ({
        label,
        stroke: colors[index % colors.length],
        fill: stacked
          ? withAlpha(colors[index % colors.length], 0.35)
          : undefined,
        width: stacked ? 1 : 2,
        spanGaps: false,
        value: (_self: uPlot, rawValue: number | null, seriesIndex: number) => {
          if (rawValue == null) {
            return "—"
          }

          const display =
            (negated[seriesIndex - 1] ?? false) ? Math.abs(rawValue) : rawValue
          return valueFormatter ? valueFormatter(display) : String(display)
        },
      })),
    ],
    axes: [
      {
        stroke: axisColor,
        font: AXIS_FONT,
        gap: 4,
        size: X_AXIS_HEIGHT,
        grid: { stroke: gridColor },
        ticks: { stroke: axisColor, size: 4 },
        values: (_self, splits, _axisIdx, _foundSpace, foundIncr) =>
          formatChartAxisTicks(splits, foundIncr),
      },
      {
        stroke: axisColor,
        font: AXIS_FONT,
        gap: 8,
        size: (_self, values) => measureAxisWidth(values),
        grid: { stroke: gridColor },
        ticks: { stroke: axisColor },
        values: (_self, ticks) => ticks.map((value) => formatAxisValue(value)),
      },
    ],
    legend: { show: false },
    bands,
    cursor: {
      show: true,
      drag: { x: true, y: false },
      focus: { prox: 24 },
    },
    scales: {
      x: { time: true },
      y: buildYScale(yRange, bidirectional),
    },
    // Extra padding keeps top/bottom tick labels from being clipped.
    padding: [14, 16, 12, 8],
  }
}
