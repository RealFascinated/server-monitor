import { useUserPreference } from "@/hooks/use-user-preference"
import { parseMetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { Preferences } from "@/lib/preferences"

export function useMetricRefreshInterval() {
  const { value: raw, setValue: setRaw } = useUserPreference(
    Preferences.METRIC_REFRESH_INTERVAL
  )

  return {
    refreshInterval: parseMetricRefreshInterval(raw),
    setRefreshInterval: setRaw,
  } as const
}
