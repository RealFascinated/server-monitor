import type { MetricValues } from "@/lib/api/user/metrics"
import type { ChartThreshold } from "@/lib/metrics/chart-thresholds"
import { formatNumber } from "@/lib/formatter"

export const TEMPERATURE_UNITS = ["celsius", "fahrenheit"] as const

export type TemperatureUnit = (typeof TEMPERATURE_UNITS)[number]

export const DEFAULT_TEMPERATURE_UNIT: TemperatureUnit = "celsius"

export const TEMPERATURE_UNIT_OPTIONS: {
  value: TemperatureUnit
  label: string
  symbol: string
}[] = [
  { value: "celsius", label: "Celsius (°C)", symbol: "°C" },
  { value: "fahrenheit", label: "Fahrenheit (°F)", symbol: "°F" },
]

const SYMBOLS: Record<TemperatureUnit, string> = {
  celsius: "°C",
  fahrenheit: "°F",
}

export function parseTemperatureUnit(value: unknown): TemperatureUnit {
  return (
    TEMPERATURE_UNITS.find((unit) => unit === value) ?? DEFAULT_TEMPERATURE_UNIT
  )
}

export function temperatureSymbol(unit: TemperatureUnit): string {
  return SYMBOLS[unit]
}

/** Stored metrics and alert thresholds are always Celsius; this converts for display. */
export function fromCelsius(celsius: number, unit: TemperatureUnit): number {
  return unit === "fahrenheit" ? celsius * 1.8 + 32 : celsius
}

/** Converts a value the user typed in their unit back to the stored Celsius. */
export function toCelsius(value: number, unit: TemperatureUnit): number {
  return unit === "fahrenheit" ? (value - 32) / 1.8 : value
}

export function convertTemperatureValues(
  values: MetricValues,
  unit: TemperatureUnit
): MetricValues {
  if (!values || unit === "celsius") {
    return values
  }

  return values.map((value) =>
    value == null ? null : fromCelsius(value, unit)
  )
}

export function convertTemperatureThresholds(
  thresholds: ChartThreshold[],
  unit: TemperatureUnit
): ChartThreshold[] {
  return unit === "celsius"
    ? thresholds
    : thresholds.map((threshold) => ({
        ...threshold,
        value: fromCelsius(threshold.value, unit),
      }))
}

/** Formats a Celsius value in the reader's unit. */
export function formatTemperature(
  celsius: number,
  unit: TemperatureUnit
): string {
  return `${formatNumber(fromCelsius(celsius, unit))} ${temperatureSymbol(unit)}`
}

/** Formatter for chart data that has already been converted to {@code unit}. */
export function convertedTemperatureFormatter(
  unit: TemperatureUnit
): (value: number) => string {
  return (value) => `${formatNumber(value)} ${temperatureSymbol(unit)}`
}
