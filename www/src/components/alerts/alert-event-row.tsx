import { Link } from "@tanstack/react-router"

import { TimestampCell } from "@/components/timestamp-cell"
import { useTemperatureUnit } from "@/hooks/use-temperature-unit"
import { OPERATOR_LABELS } from "@/lib/alerts/summary"
import { formatAlertValue } from "@/lib/alerts/units"
import type { AlertEvent } from "@/lib/api/user/alerts"
import { formatDurationSeconds } from "@/lib/formatter"
import { cn } from "@/lib/utils"

function eventDurationSeconds(event: AlertEvent): number {
  const end = event.resolvedAt ? new Date(event.resolvedAt) : new Date()
  return Math.max(
    0,
    Math.round((end.getTime() - new Date(event.startedAt).getTime()) / 1000)
  )
}

function labelsText(labels: Record<string, string>): string | null {
  const values = Object.values(labels)
  return values.length > 0 ? values.join(" · ") : null
}

function AlertEventRow({ event }: { event: AlertEvent }) {
  const { temperatureUnit } = useTemperatureUnit()
  const firing = event.resolvedAt == null
  const labels = labelsText(event.labels)

  return (
    <li
      className={cn(
        "flex flex-col gap-2 rounded-lg border bg-card p-4",
        firing ? "border-error/40" : "border-border"
      )}
    >
      <div className="flex flex-wrap items-center gap-2">
        <span
          className={cn(
            "rounded-full px-2 py-0.5 text-[11px] font-medium",
            firing ? "bg-error/10 text-error" : "bg-success/10 text-success"
          )}
        >
          {firing ? "Firing" : "Resolved"}
        </span>
        <p className="text-sm font-semibold text-foreground">
          {event.ruleName || event.metricLabel}
        </p>
        {event.serverName ? (
          <Link
            to="/servers/$serverId"
            params={{ serverId: String(event.serverId) }}
            className="text-sm text-muted-foreground transition-colors hover:text-brand"
          >
            {event.serverName}
          </Link>
        ) : null}
        {labels ? (
          <span className="font-mono text-xs text-muted-foreground">
            {labels}
          </span>
        ) : null}
      </div>

      <p className="text-sm text-muted-foreground">
        {event.unit === "STATE"
          ? `${event.metricLabel} was unhealthy.`
          : `${event.metricLabel} was ${OPERATOR_LABELS[event.operator]} ${formatAlertValue(event.unit, event.threshold, temperatureUnit)}, peaking at ${formatAlertValue(event.unit, event.peakValue, temperatureUnit)}.`}
      </p>

      <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
        <span>
          Started <TimestampCell iso={event.startedAt} />
        </span>
        <span>
          {firing ? "Firing for" : "Lasted"}{" "}
          {formatDurationSeconds(eventDurationSeconds(event))}
        </span>
        {event.notifyError ? (
          <span className="text-error">
            Notification failed: {event.notifyError}
          </span>
        ) : null}
      </p>
    </li>
  )
}

export { AlertEventRow }
