import { fromAlertInputValue, toAlertInputValue } from "@/lib/alerts/units"
import type {
  AlertMetric,
  AlertOperator,
  AlertRule,
  AlertRuleRequest,
  AlertSeverity,
} from "@/lib/api/user/alerts"
import type { TemperatureUnit } from "@/lib/metrics/temperature"

/** On/off gauges (server up, drive health) are 1 = healthy, so the only condition is "below 1", never averaged. */
export function isStateMetric(unit: AlertMetric["unit"] | undefined): boolean {
  return unit === "STATE"
}

export const MAX_WINDOW_SECONDS = 86_400

/** Form state. Numbers are kept as strings, in the unit the user types them in. */
export type AlertDraft = {
  name: string
  metric: string
  labelFilter: Record<string, string>
  operator: AlertOperator
  threshold: string
  recoveryThreshold: string
  windowSeconds: number
  severity: AlertSeverity
  repeatIntervalSeconds: number
  enabled: boolean
}

export type AlertDraftErrors = Partial<
  Record<"metric" | "threshold" | "recoveryThreshold" | "name", string>
>

export const EMPTY_DRAFT: AlertDraft = {
  name: "",
  metric: "",
  labelFilter: {},
  operator: "MORE_THAN",
  threshold: "",
  recoveryThreshold: "",
  windowSeconds: 0,
  severity: "WARNING",
  repeatIntervalSeconds: 0,
  enabled: true,
}

/** Picking a metric resets everything that depends on it to that metric's suggested defaults. */
export function draftForMetric(
  current: AlertDraft,
  metric: AlertMetric,
  temperatureUnit: TemperatureUnit
): AlertDraft {
  return {
    ...current,
    metric: metric.metric,
    labelFilter: {},
    operator: metric.defaultOperator,
    threshold: String(
      toAlertInputValue(metric.unit, metric.defaultThreshold, temperatureUnit)
    ),
    recoveryThreshold: "",
    windowSeconds: isStateMetric(metric.unit) ? 0 : current.windowSeconds,
  }
}

export function ruleToDraft(
  rule: AlertRule,
  temperatureUnit: TemperatureUnit
): AlertDraft {
  return {
    name: rule.name ?? "",
    metric: rule.metric,
    labelFilter: rule.labelFilter,
    operator: rule.operator,
    threshold: String(
      toAlertInputValue(rule.unit, rule.threshold, temperatureUnit)
    ),
    recoveryThreshold:
      rule.recoveryThreshold == null
        ? ""
        : String(
            toAlertInputValue(
              rule.unit,
              rule.recoveryThreshold,
              temperatureUnit
            )
          ),
    windowSeconds: rule.windowSeconds,
    severity: rule.severity,
    repeatIntervalSeconds: rule.repeatIntervalSeconds ?? 0,
    enabled: rule.enabled,
  }
}

function parseNumber(raw: string): number | null {
  if (raw.trim() === "") return null
  const value = Number(raw)
  return Number.isFinite(value) ? value : null
}

/** Mirrors the server-side rule validation so most mistakes are caught before the request. */
export function validateDraft(
  draft: AlertDraft,
  unit: AlertMetric["unit"] | undefined,
  temperatureUnit: TemperatureUnit
): AlertDraftErrors {
  const errors: AlertDraftErrors = {}

  if (draft.metric === "") errors.metric = "Choose a metric"
  if (draft.name.length > 64) errors.name = "Name must be at most 64 characters"

  const threshold = parseNumber(draft.threshold)
  if (threshold == null) {
    errors.threshold = "Enter a number"
  }

  if (draft.recoveryThreshold.trim() !== "") {
    const recovery = parseNumber(draft.recoveryThreshold)
    if (recovery == null) {
      errors.recoveryThreshold = "Enter a number"
    } else if (threshold != null && unit) {
      const stored = fromAlertInputValue(unit, threshold, temperatureUnit)
      const storedRecovery = fromAlertInputValue(
        unit,
        recovery,
        temperatureUnit
      )
      const wrongSide =
        draft.operator === "MORE_THAN"
          ? storedRecovery > stored
          : storedRecovery < stored
      if (wrongSide) {
        errors.recoveryThreshold =
          draft.operator === "MORE_THAN"
            ? "Must not be above the alert threshold"
            : "Must not be below the alert threshold"
      }
    }
  }

  return errors
}

export function draftToRequest(
  groupId: number,
  draft: AlertDraft,
  unit: AlertMetric["unit"],
  temperatureUnit: TemperatureUnit
): AlertRuleRequest {
  const threshold = parseNumber(draft.threshold) ?? 0
  const recovery = parseNumber(draft.recoveryThreshold)

  return {
    groupId,
    name: draft.name.trim() === "" ? null : draft.name.trim(),
    metric: draft.metric,
    labelFilter: draft.labelFilter,
    operator: draft.operator,
    threshold: fromAlertInputValue(unit, threshold, temperatureUnit),
    recoveryThreshold:
      recovery == null
        ? null
        : fromAlertInputValue(unit, recovery, temperatureUnit),
    windowSeconds: draft.windowSeconds,
    severity: draft.severity,
    repeatIntervalSeconds:
      draft.repeatIntervalSeconds > 0 ? draft.repeatIntervalSeconds : null,
    enabled: draft.enabled,
  }
}

/** A copy of an existing rule, ready to create in the same group. */
export function ruleToCopyRequest(rule: AlertRule): AlertRuleRequest {
  return {
    groupId: rule.groupId,
    name: `${rule.name || rule.metricLabel} (copy)`.slice(0, 64),
    metric: rule.metric,
    labelFilter: rule.labelFilter,
    operator: rule.operator,
    threshold: rule.threshold,
    recoveryThreshold: rule.recoveryThreshold ?? null,
    windowSeconds: rule.windowSeconds,
    severity: rule.severity,
    repeatIntervalSeconds: rule.repeatIntervalSeconds ?? null,
    enabled: false,
  }
}
