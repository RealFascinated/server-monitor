import {
  getCoreRowModel,
  useLegacyTable as useReactTable,
} from "@tanstack/react-table/legacy"
import type { LegacyColumnDef as ColumnDef } from "@tanstack/react-table/legacy"
import { Link } from "@tanstack/react-router"
import { Bell, BellOff } from "lucide-react"
import { useMemo } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { TimestampCell } from "@/components/timestamp-cell"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import {
  useSilenceAlertEvent,
  useUnsilenceAlertEvent,
} from "@/hooks/use-alert-events"
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

function isSilenced(event: AlertEvent): boolean {
  return event.resolvedAt == null && event.silencedAt != null
}

function EventStatus({ event }: { event: AlertEvent }) {
  const { temperatureUnit } = useTemperatureUnit()
  const silenced = isSilenced(event)
  const firing = event.resolvedAt == null && !silenced
  const badge = (
    <span
      className={cn(
        "inline-flex rounded-sm px-2 py-0.5 text-xs font-medium",
        firing
          ? "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300"
          : "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300"
      )}
    >
      {firing ? "Firing" : silenced ? "Silenced" : "Resolved"}
    </span>
  )

  if (!silenced || event.silencedValue == null) {
    return badge
  }
  return (
    <SimpleTooltip
      content={`Quiet until the value is ${OPERATOR_LABELS[event.operator]} ${formatAlertValue(event.unit, event.silencedValue, temperatureUnit)}`}
    >
      {badge}
    </SimpleTooltip>
  )
}

function EventName({ event }: { event: AlertEvent }) {
  const labels = Object.values(event.labels).join(" · ")

  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <span className="truncate font-medium text-foreground">
        {event.ruleName || event.metricLabel}
      </span>
      {labels ? (
        <span className="truncate font-mono text-xs text-muted-foreground">
          {labels}
        </span>
      ) : null}
      {event.notifyError ? (
        <span className="truncate text-xs text-error">
          Notification failed: {event.notifyError}
        </span>
      ) : null}
    </div>
  )
}

function EventPeak({ event }: { event: AlertEvent }) {
  const { temperatureUnit } = useTemperatureUnit()

  if (event.unit === "STATE") {
    return <span className="text-muted-foreground">Unhealthy</span>
  }
  return (
    <span className="text-muted-foreground">
      {formatAlertValue(event.unit, event.peakValue, temperatureUnit)}
    </span>
  )
}

function EventActions({ event }: { event: AlertEvent }) {
  const silence = useSilenceAlertEvent()
  const unsilence = useUnsilenceAlertEvent()
  const silenced = isSilenced(event)

  if (event.resolvedAt != null) {
    return null
  }
  return (
    <div className="flex justify-end">
      <SimpleTooltip
        content={
          silenced
            ? "Notify me about this again"
            : "Stop notifying until the value gets worse than it is now"
        }
      >
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={silenced ? "Unsilence alert" : "Silence until it changes"}
          disabled={silence.isPending || unsilence.isPending}
          onClick={() =>
            silenced ? unsilence.mutate(event.id) : silence.mutate(event.id)
          }
        >
          {silenced ? <Bell /> : <BellOff />}
        </Button>
      </SimpleTooltip>
    </div>
  )
}

function AlertEventsTable({ events }: { events: AlertEvent[] }) {
  const columns = useMemo<ColumnDef<AlertEvent>[]>(
    () => [
      {
        id: "status",
        header: "Status",
        cell: ({ row }) => <EventStatus event={row.original} />,
      },
      {
        id: "alert",
        header: "Alert",
        cell: ({ row }) => <EventName event={row.original} />,
      },
      {
        id: "server",
        header: "Server",
        cell: ({ row }) =>
          row.original.serverName ? (
            <Link
              to="/servers/$serverId"
              params={{ serverId: String(row.original.serverId) }}
              className="text-muted-foreground transition-colors hover:text-brand"
            >
              {row.original.serverName}
            </Link>
          ) : null,
      },
      {
        id: "peak",
        header: "Peak",
        cell: ({ row }) => <EventPeak event={row.original} />,
      },
      {
        id: "started",
        header: "Started",
        cell: ({ row }) => (
          <TimestampCell
            iso={row.original.startedAt}
            className="text-muted-foreground"
          />
        ),
      },
      {
        id: "duration",
        header: "Duration",
        cell: ({ row }) => (
          <span className="text-muted-foreground">
            {formatDurationSeconds(eventDurationSeconds(row.original))}
          </span>
        ),
      },
      {
        id: "actions",
        header: () => <span className="sr-only">Actions</span>,
        cell: ({ row }) => <EventActions event={row.original} />,
      },
    ],
    []
  )

  const table = useReactTable({
    data: events,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  return <DataTable table={table} />
}

export { AlertEventsTable }
