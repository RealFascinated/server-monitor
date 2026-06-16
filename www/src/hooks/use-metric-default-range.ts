import { useUserPreference } from "@/hooks/use-user-preference"
import { parseMetricRange } from "@/lib/metrics/range"
import type { MetricTimeRange } from "@/lib/metrics/range"
import { Preferences } from "@/lib/preferences"

export function useMetricDefaultRange() {
  const { value: raw, setValue: setRaw } = useUserPreference(
    Preferences.METRIC_DEFAULT_RANGE
  )

  return {
    defaultRange: parseMetricRange(raw),
    setDefaultRange: (value: MetricTimeRange) => setRaw(value),
  } as const
}
