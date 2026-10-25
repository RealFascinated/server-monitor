import { useMemo } from "react"

import { Callout } from "@/components/callout"
import { MetricChart } from "@/components/metrics/metric-chart"
import { Spinner } from "@/components/spinner"
import { useAlertPreview } from "@/hooks/use-alert-preview"
import { buildPreviewChartData } from "@/lib/alerts/preview"
import { formatAlertValue } from "@/lib/alerts/units"
import type {
  AlertOperator,
  AlertSeverity,
  AlertUnit,
} from "@/lib/api/user/alerts"
import { MetricsChartAnnotationsProvider } from "@/lib/metrics/chart-annotations"
import {
  convertedTemperatureFormatter,
  fromCelsius,
} from "@/lib/metrics/temperature"
import type { TemperatureUnit } from "@/lib/metrics/temperature"

export type PreviewCondition = {
  serverId: number
  metric: string
  labelFilter: Record<string, string>
  operator: AlertOperator
  /** Stored units (Celsius, bytes, ...). Null while the threshold is not a valid number. */
  threshold: number | null
  recoveryThreshold: number | null
  windowSeconds: number
  severity: AlertSeverity
  unit: AlertUnit
  metricLabel: string
}

const RANGE_HOURS = 24 * 7

function AlertPreviewChart({
  condition,
  temperatureUnit,
}: {
  condition: PreviewCondition
  temperatureUnit: TemperatureUnit
}) {
  const { data, error, isFetching } = useAlertPreview(
    condition.threshold == null
      ? null
      : {
          serverId: condition.serverId,
          metric: condition.metric,
          labelFilter: condition.labelFilter,
          operator: condition.operator,
          threshold: condition.threshold,
          recoveryThreshold: condition.recoveryThreshold,
          windowSeconds: condition.windowSeconds,
          rangeHours: RANGE_HOURS,
        }
  )

  const celsius = condition.unit === "TEMPERATURE"
  const chart = useMemo(
    () =>
      data
        ? buildPreviewChartData(
            data,
            condition.metricLabel,
            celsius ? (value) => fromCelsius(value, temperatureUnit) : undefined
          )
        : null,
    [data, condition.metricLabel, celsius, temperatureUnit]
  )

  if (condition.threshold == null) {
    return (
      <p className="text-sm text-muted-foreground">
        Enter a threshold to preview this alert against the last 7 days.
      </p>
    )
  }

  if (error && !data) {
    return (
      <Callout type="warning" title="Preview unavailable">
        {error.message || "The metric history could not be loaded."}
      </Callout>
    )
  }

  if (!chart) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner />
      </div>
    )
  }

  if (chart.data[0].length === 0) {
    return (
      <Callout title="No data yet">
        This server has not reported {condition.metricLabel.toLowerCase()} in
        the last 7 days, so there is nothing to preview.
      </Callout>
    )
  }

  const threshold = celsius
    ? fromCelsius(condition.threshold, temperatureUnit)
    : condition.threshold
  const fired = data?.firingCount ?? 0

  return (
    <div className="flex flex-col gap-2">
      <p className="flex items-center gap-2 text-sm text-foreground">
        {fired === 0
          ? "Would not have fired in the last 7 days."
          : `Would have fired ${fired} ${fired === 1 ? "time" : "times"} in the last 7 days.`}
        {isFetching ? <Spinner /> : null}
      </p>
      <div className="relative h-64">
        <MetricsChartAnnotationsProvider spans={chart.spans}>
          <MetricChart
            data={chart.data}
            labels={chart.labels}
            valueFormatter={
              celsius
                ? convertedTemperatureFormatter(temperatureUnit)
                : (value) =>
                    formatAlertValue(condition.unit, value, temperatureUnit)
            }
            thresholds={[
              {
                value: threshold,
                level:
                  condition.severity === "CRITICAL" ? "critical" : "warning",
              },
            ]}
          />
        </MetricsChartAnnotationsProvider>
      </div>
      {data?.truncated ? (
        <p className="text-xs text-muted-foreground">
          Showing the busiest series only.
        </p>
      ) : null}
    </div>
  )
}

export { AlertPreviewChart }
