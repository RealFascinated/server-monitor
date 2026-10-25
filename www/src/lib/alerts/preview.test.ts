import { describe, expect, it } from "vitest"

import { buildPreviewChartData } from "@/lib/alerts/preview"
import type { AlertPreview } from "@/lib/api/user/alerts"

const preview: AlertPreview = {
  unit: "PERCENT",
  from: 0,
  to: 120,
  stepSeconds: 60,
  firingCount: 1,
  truncated: false,
  series: [
    {
      labels: {},
      timestamps: [0, 60, 120],
      values: [10, 95, null],
      firing: [{ from: 60, to: 120, ongoing: true, peak: 95 }],
    },
    {
      labels: { disk: "/data" },
      timestamps: [60, 120],
      values: [5, 6],
      firing: [],
    },
  ],
}

describe("buildPreviewChartData", () => {
  it("aligns series on a shared sorted x axis", () => {
    const { data, labels } = buildPreviewChartData(preview, "CPU usage")

    expect(data[0]).toEqual([0, 60, 120])
    expect(data[1]).toEqual([10, 95, null])
    expect(data[2]).toEqual([null, 5, 6])
    expect(labels).toEqual(["CPU usage", "/data"])
  })

  it("converts values and collects firing spans", () => {
    const { data, spans } = buildPreviewChartData(preview, "x", (v) => v * 2)

    expect(data[1]).toEqual([20, 190, null])
    expect(spans).toEqual([{ from: 60, to: 120, ongoing: true }])
  })
})
