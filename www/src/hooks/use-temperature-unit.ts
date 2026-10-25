import { useUserPreference } from "@/hooks/use-user-preference"
import { parseTemperatureUnit } from "@/lib/metrics/temperature"
import type { TemperatureUnit } from "@/lib/metrics/temperature"
import { Preferences } from "@/lib/preferences"

export function useTemperatureUnit() {
  const { value: raw, setValue: setRaw } = useUserPreference(
    Preferences.TEMPERATURE_UNIT
  )

  return {
    temperatureUnit: parseTemperatureUnit(raw),
    setTemperatureUnit: (value: TemperatureUnit) => setRaw(value),
  } as const
}
