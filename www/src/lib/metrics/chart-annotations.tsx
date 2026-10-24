import { createContext, useContext, useMemo } from "react"
import type { ReactNode } from "react"
import uPlot from "uplot"

import type { IncidentResponse } from "@/lib/api/user/incidents"
import type { ResolvedTheme } from "@/lib/theme/context"

/** An outage span in epoch seconds, matching the charts' x scale. */
export type ChartIncidentSpan = {
  from: number
  to: number
  ongoing: boolean
}

const EMPTY_SPANS: ChartIncidentSpan[] = []

const MetricsChartAnnotationsContext =
  createContext<ChartIncidentSpan[]>(EMPTY_SPANS)

/** Keeps incidents that overlap the visible window and converts them to seconds. */
export function incidentsToSpans(
  incidents: IncidentResponse[],
  window: { from: number; to: number },
  nowMs = Date.now()
): ChartIncidentSpan[] {
  const spans: ChartIncidentSpan[] = []

  for (const incident of incidents) {
    const from = Math.floor(new Date(incident.startedAt).getTime() / 1000)
    const ongoing = incident.resolvedAt == null
    const to = Math.ceil(
      (ongoing ? nowMs : new Date(incident.resolvedAt!).getTime()) / 1000
    )

    if (Number.isNaN(from) || Number.isNaN(to) || to < window.from) {
      continue
    }

    if (from > window.to) {
      continue
    }

    spans.push({ from, to, ongoing })
  }

  return spans
}

function MetricsChartAnnotationsProvider({
  spans,
  children,
}: {
  spans: ChartIncidentSpan[]
  children: ReactNode
}) {
  const key = spans.map((span) => `${span.from}:${span.to}`).join("|")
  // Re-memoise on content, not identity, so refetches don't rebuild every chart.
  const stable = useMemo(() => spans, [key])

  return (
    <MetricsChartAnnotationsContext.Provider value={stable}>
      {children}
    </MetricsChartAnnotationsContext.Provider>
  )
}

function useMetricsChartIncidents() {
  return useContext(MetricsChartAnnotationsContext)
}

const INCIDENT_FILL: Record<ResolvedTheme, string> = {
  light: "rgba(220, 38, 38, 0.09)",
  dark: "rgba(248, 113, 113, 0.12)",
}

const INCIDENT_EDGE: Record<ResolvedTheme, string> = {
  light: "rgba(220, 38, 38, 0.45)",
  dark: "rgba(248, 113, 113, 0.55)",
}

/** Shades outage periods behind the series so gaps in data are explained. */
export function createIncidentDrawHook(
  spans: ChartIncidentSpan[],
  theme: ResolvedTheme
): (u: uPlot) => void {
  return (u) => {
    if (spans.length === 0) {
      return
    }

    const { ctx, bbox } = u
    const left = bbox.left
    const right = bbox.left + bbox.width

    ctx.save()
    ctx.beginPath()
    ctx.rect(bbox.left, bbox.top, bbox.width, bbox.height)
    ctx.clip()

    for (const span of spans) {
      const x0 = Math.max(left, u.valToPos(span.from, "x", true))
      const x1 = Math.min(right, u.valToPos(span.to, "x", true))
      if (!(x1 > x0)) {
        continue
      }

      ctx.fillStyle = INCIDENT_FILL[theme]
      ctx.fillRect(x0, bbox.top, x1 - x0, bbox.height)

      ctx.fillStyle = INCIDENT_EDGE[theme]
      ctx.fillRect(x0, bbox.top, uPlot.pxRatio, bbox.height)
    }

    ctx.restore()
  }
}

export { MetricsChartAnnotationsProvider, useMetricsChartIncidents }
