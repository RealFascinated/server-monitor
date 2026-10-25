import type { AlertUnit } from "@/lib/api/user/alerts"
import {
  formatCount,
  formatMemoryBytes,
  formatMilliseconds,
  formatNumber,
  formatPercentValue,
  formatRate,
  formatWatts,
} from "@/lib/formatter"
import {
  formatTemperature,
  fromCelsius,
  temperatureSymbol,
  toCelsius,
} from "@/lib/metrics/temperature"
import type { TemperatureUnit } from "@/lib/metrics/temperature"

const MEGABYTE = 1024 * 1024

/** Formats a stored value (Celsius, bytes, ...) with the existing chart formatters. */
export function formatAlertValue(
  unit: AlertUnit,
  value: number,
  temperatureUnit: TemperatureUnit
): string {
  switch (unit) {
    case "PERCENT":
      return formatPercentValue(value)
    case "BYTES":
      return formatMemoryBytes(value)
    case "BYTES_PER_SECOND":
      return formatRate(value)
    case "MILLISECONDS":
      return formatMilliseconds(value)
    case "TEMPERATURE":
      return formatTemperature(value, temperatureUnit)
    case "WATTS":
      return formatWatts(value)
    case "COUNT":
      return formatCount(value)
    case "STATE":
      return value >= 0.5 ? "healthy" : "unhealthy"
    case "NONE":
      return formatNumber(value)
  }
}

/** The unit the user types a threshold in: bytes are entered as MB, temperatures in their own unit. */
export function alertInputSuffix(
  unit: AlertUnit,
  temperatureUnit: TemperatureUnit
): string {
  switch (unit) {
    case "PERCENT":
      return "%"
    case "BYTES":
      return "MB"
    case "BYTES_PER_SECOND":
      return "MB/s"
    case "MILLISECONDS":
      return "ms"
    case "TEMPERATURE":
      return temperatureSymbol(temperatureUnit)
    case "WATTS":
      return "W"
    case "COUNT":
    case "STATE":
    case "NONE":
      return ""
  }
}

function roundForInput(value: number): number {
  return Number(value.toFixed(2))
}

/** Stored value to what the threshold input shows. */
export function toAlertInputValue(
  unit: AlertUnit,
  stored: number,
  temperatureUnit: TemperatureUnit
): number {
  switch (unit) {
    case "BYTES":
    case "BYTES_PER_SECOND":
      return roundForInput(stored / MEGABYTE)
    case "TEMPERATURE":
      return roundForInput(fromCelsius(stored, temperatureUnit))
    default:
      return stored
  }
}

/** What the user typed back to the stored value. */
export function fromAlertInputValue(
  unit: AlertUnit,
  input: number,
  temperatureUnit: TemperatureUnit
): number {
  switch (unit) {
    case "BYTES":
    case "BYTES_PER_SECOND":
      return input * MEGABYTE
    case "TEMPERATURE":
      return toCelsius(input, temperatureUnit)
    default:
      return input
  }
}
