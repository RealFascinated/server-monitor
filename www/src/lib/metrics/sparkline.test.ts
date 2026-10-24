import { describe, expect, it } from "vitest"

import type { ServerMetricsResponse } from "@/lib/api/user/metrics"
import {
  buildServerSparklines,
  downsampleValues,
  sparklineHasData,
  sparklineStats,
} from "@/lib/metrics/sparkline"

describe("downsampleValues", () => {
  it("returns short series unchanged", () => {
    const values = [1, null, 3]
    expect(downsampleValues(values, 10)).toBe(values)
  })

  it("averages buckets and ignores nulls", () => {
    expect(downsampleValues([2, 4, null, null, 6, 8], 3)).toEqual([3, null, 7])
  })

  it("never exceeds the requested number of points", () => {
    const values = Array.from({ length: 1000 }, (_, index) => index)
    expect(downsampleValues(values, 48)).toHaveLength(48)
  })
})

describe("sparklineStats", () => {
  it("summarises finite values and reports the latest", () => {
    expect(sparklineStats([10, null, 30, 20])).toEqual({
      min: 10,
      max: 30,
      avg: 20,
      latest: 20,
    })
  })

  it("returns null when there is no data", () => {
    expect(sparklineStats([null, null])).toBeNull()
    expect(sparklineHasData([null])).toBe(false)
    expect(sparklineHasData([0])).toBe(true)
  })
})

describe("buildServerSparklines", () => {
  it("converts memory bytes to a percentage of total", () => {
    const metrics = {
      id: 1,
      from: 0,
      to: 10,
      step: 5,
      timestamps: [0, 5],
      host: {
        cpuUsage: [12, 18],
        memUsage: [50, 25],
        memTotal: [100, 100],
      },
    } satisfies ServerMetricsResponse

    expect(buildServerSparklines(metrics)).toEqual({
      cpu: [12, 18],
      memory: [50, 25],
    })
  })

  it("yields null for points without a usable total", () => {
    const metrics = {
      id: 1,
      from: 0,
      to: 10,
      step: 5,
      timestamps: [0, 5],
      host: { memUsage: [50, 25], memTotal: [0, null] },
    } satisfies ServerMetricsResponse

    expect(buildServerSparklines(metrics).memory).toEqual([null, null])
  })

  it("handles a missing host section", () => {
    const metrics: ServerMetricsResponse = {
      id: 1,
      from: 0,
      to: 10,
      step: 5,
      timestamps: [],
    }

    expect(buildServerSparklines(metrics)).toEqual({ cpu: [], memory: [] })
  })
})
