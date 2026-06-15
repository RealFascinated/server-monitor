import { z } from "zod"

import type { MetricTimeRange } from "@/lib/metrics/range"
import { METRIC_RANGES } from "@/lib/metrics/range"
import { getStoredDefaultMetricRange } from "@/lib/metrics/default-range"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"

export function metricRangeSearchSchema(overrideDefault?: MetricTimeRange) {
  return z
    .object({
      range: z.enum(METRIC_RANGES).optional(),
      from: z.coerce.number().int().positive().optional(),
      to: z.coerce.number().int().positive().optional(),
    })
    .transform((search): MetricTimeWindow => {
      const { from, to, range } = search

      if (from != null && to != null && from < to) {
        return { kind: "custom", from, to }
      }

      const defaultRange = overrideDefault ?? getStoredDefaultMetricRange()
      return { kind: "preset", range: range ?? defaultRange }
    })
}
