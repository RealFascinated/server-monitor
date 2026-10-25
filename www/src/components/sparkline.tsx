import { useId, useMemo } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { formatPercent } from "@/lib/formatter"
import { sparklineStats } from "@/lib/metrics/sparkline"
import type { SparklineValues } from "@/lib/metrics/sparkline"
import { cn } from "@/lib/utils"

type SparklineProps = {
  values: SparklineValues
  /** Accessible name, e.g. "CPU, last hour". */
  label: string
  className?: string
  width?: number
  height?: number
}

const MIN_DOMAIN_MAX = 25

function buildPaths(
  values: SparklineValues,
  width: number,
  height: number,
  domainMax: number
) {
  const padding = 2
  const innerHeight = height - padding * 2
  const step = values.length > 1 ? width / (values.length - 1) : 0

  const segments: { x: number; y: number }[][] = []
  let current: { x: number; y: number }[] = []

  values.forEach((value, index) => {
    if (value == null || !Number.isFinite(value)) {
      if (current.length > 0) {
        segments.push(current)
        current = []
      }
      return
    }

    const clamped = Math.min(Math.max(value, 0), domainMax)
    current.push({
      x: index * step,
      y: padding + innerHeight - (clamped / domainMax) * innerHeight,
    })
  })

  if (current.length > 0) {
    segments.push(current)
  }

  const line = segments
    .map((segment) =>
      segment
        .map(
          (point, index) =>
            `${index === 0 ? "M" : "L"}${point.x.toFixed(1)} ${point.y.toFixed(1)}`
        )
        .join(" ")
    )
    .join(" ")

  const area = segments
    .filter((segment) => segment.length > 1)
    .map((segment) => {
      const first = segment[0]
      const last = segment[segment.length - 1]
      const top = segment
        .map((point) => `L${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
        .join(" ")
      return `M${first.x.toFixed(1)} ${height} ${top} L${last.x.toFixed(1)} ${height} Z`
    })
    .join(" ")

  return { line, area }
}

/**
 * Tiny dependency-free trend line for percentage series. Draws in
 * `currentColor`, so callers set the colour with a text class.
 */
function Sparkline({
  values,
  label,
  className,
  width = 72,
  height = 24,
}: SparklineProps) {
  const gradientId = useId()
  const stats = useMemo(() => sparklineStats(values), [values])

  const paths = useMemo(() => {
    if (!stats) {
      return null
    }

    const domainMax = Math.min(100, Math.max(MIN_DOMAIN_MAX, stats.max * 1.2))
    return buildPaths(values, width, height, domainMax)
  }, [stats, values, width, height])

  if (!stats || !paths) {
    return null
  }

  const description = `${label}: now ${formatPercent(stats.latest)}, average ${formatPercent(stats.avg)}, peak ${formatPercent(stats.max)}`

  return (
    <SimpleTooltip content={description}>
      <svg
        role="img"
        aria-label={description}
        viewBox={`0 0 ${width} ${height}`}
        width={width}
        height={height}
        preserveAspectRatio="none"
        className={cn("shrink-0 cursor-help overflow-visible", className)}
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="currentColor" stopOpacity="0.28" />
            <stop offset="100%" stopColor="currentColor" stopOpacity="0" />
          </linearGradient>
        </defs>
        {paths.area ? (
          <path d={paths.area} fill={`url(#${gradientId})`} />
        ) : null}
        <path
          d={paths.line}
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      </svg>
    </SimpleTooltip>
  )
}

export { Sparkline }
