import { useEffect } from "react"

import { useUserPreference } from "@/hooks/use-user-preference"
import { METRIC_DEFAULT_RANGE_STORAGE_KEY } from "@/lib/metrics/default-range"
import { parseMetricRange } from "@/lib/metrics/range"
import type { MetricTimeRange } from "@/lib/metrics/range"
import { Preferences } from "@/lib/preferences"

export function useMetricDefaultRange() {
  const { value: raw, setValue: setRaw } = useUserPreference(
    Preferences.METRIC_DEFAULT_RANGE
  )
  const defaultRange = parseMetricRange(raw)

  useEffect(() => {
    localStorage.setItem(METRIC_DEFAULT_RANGE_STORAGE_KEY, defaultRange)
  }, [defaultRange])

  return {
    defaultRange,
    setDefaultRange: (value: MetricTimeRange) => {
      localStorage.setItem(METRIC_DEFAULT_RANGE_STORAGE_KEY, value)
      setRaw(value)
    },
  } as const
}
