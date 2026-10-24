import { describe, expect, it } from "vitest"

import { incidentsToSpans } from "@/lib/metrics/chart-annotations"

const iso = (seconds: number) => new Date(seconds * 1000).toISOString()

describe("incidentsToSpans", () => {
  const window = { from: 1_000, to: 2_000 }

  it("keeps incidents overlapping the window", () => {
    const spans = incidentsToSpans(
      [
        { id: 1, serverId: 1, startedAt: iso(1_200), resolvedAt: iso(1_300) },
        { id: 2, serverId: 1, startedAt: iso(500), resolvedAt: iso(1_100) },
        { id: 3, serverId: 1, startedAt: iso(1_900), resolvedAt: iso(2_500) },
      ],
      window
    )

    expect(spans.map((span) => [span.from, span.to])).toEqual([
      [1_200, 1_300],
      [500, 1_100],
      [1_900, 2_500],
    ])
  })

  it("drops incidents entirely outside the window", () => {
    const spans = incidentsToSpans(
      [
        { id: 1, serverId: 1, startedAt: iso(100), resolvedAt: iso(200) },
        { id: 2, serverId: 1, startedAt: iso(3_000), resolvedAt: iso(3_100) },
      ],
      window
    )

    expect(spans).toEqual([])
  })

  it("extends ongoing incidents to now", () => {
    const [span] = incidentsToSpans(
      [{ id: 1, serverId: 1, startedAt: iso(1_500), resolvedAt: null }],
      window,
      1_800_000
    )

    expect(span).toEqual({ from: 1_500, to: 1_800, ongoing: true })
  })
})
