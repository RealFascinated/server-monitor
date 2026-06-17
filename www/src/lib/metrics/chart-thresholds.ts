import type { ResolvedTheme } from "@/lib/theme/context"
import uPlot from "uplot"

export type ChartThresholdLevel = "warning" | "critical"

export type ChartThreshold = {
  value: number
  level: ChartThresholdLevel
}

const THRESHOLD_COLORS: Record<
  ResolvedTheme,
  Record<ChartThresholdLevel, string>
> = {
  light: {
    warning: "#D4A030",
    critical: "#DC6B6B",
  },
  dark: {
    warning: "#FADE2A",
    critical: "#F2495C",
  },
}

export const PERCENT_THRESHOLDS: ChartThreshold[] = [
  { value: 80, level: "warning" },
  { value: 95, level: "critical" },
]

export const BATTERY_THRESHOLDS: ChartThreshold[] = [
  { value: 20, level: "warning" },
  { value: 5, level: "critical" },
]

export const TEMPERATURE_THRESHOLDS: ChartThreshold[] = [
  { value: 70, level: "warning" },
  { value: 85, level: "critical" },
]

function thresholdStroke(pxRatio: number) {
  const scale = (value: number) => value * pxRatio

  return {
    lineWidth: scale(1),
    dash: [5, 4].map(scale),
  }
}

export function createThresholdDrawHook(
  thresholds: ChartThreshold[],
  theme: ResolvedTheme,
  scale: "y" | "y2" = "y"
): (u: uPlot) => void {
  return (u) => {
    if (thresholds.length === 0) {
      return
    }

    const { ctx, bbox } = u
    const xLeft = bbox.left
    const xRight = bbox.left + bbox.width
    const yTop = bbox.top
    const yBottom = bbox.top + bbox.height

    const stroke = thresholdStroke(uPlot.pxRatio)

    ctx.save()

    for (const threshold of thresholds) {
      const yPos = u.valToPos(threshold.value, scale, true)
      if (yPos < yTop || yPos > yBottom) {
        continue
      }

      const color = THRESHOLD_COLORS[theme][threshold.level]

      ctx.beginPath()
      ctx.setLineDash(stroke.dash)
      ctx.strokeStyle = color
      ctx.lineWidth = stroke.lineWidth
      ctx.lineCap = "round"
      ctx.moveTo(xLeft, yPos)
      ctx.lineTo(xRight, yPos)
      ctx.stroke()
      ctx.setLineDash([])
    }

    ctx.restore()
  }
}
