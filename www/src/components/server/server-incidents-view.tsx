import { getCoreRowModel, useReactTable } from "@tanstack/react-table"
import type { ColumnDef } from "@tanstack/react-table"
import { useEffect, useMemo, useState } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { Pagination } from "@/components/pagination"
import { DataTable } from "@/components/ui/data-table"
import type { Page } from "@/lib/api/pagination"
import type { IncidentResponse } from "@/lib/api/user/incidents"
import {
  getIncidentDurationMs,
  getIncidentStatus,
} from "@/lib/api/user/incidents"
import {
  formatDate,
  formatDateWithRelative,
  formatDurationSeconds,
} from "@/lib/formatter"
import type { PageSearchParams } from "@/lib/schemas/pagination"
import { INCIDENT_STATUS_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

type ServerIncidentsViewProps = {
  page: Page<IncidentResponse>
  pagination: PageSearchParams
  onPageChange: (page: number) => void
  onPageSizeChange: (count: number) => void
}

const incidentStatusStyles = {
  ongoing: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
  resolved:
    "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300",
} as const

function IncidentStatusBadge({ incident }: { incident: IncidentResponse }) {
  const status = getIncidentStatus(incident)

  return (
    <SimpleTooltip content={INCIDENT_STATUS_TOOLTIPS[status]}>
      <span
        className={cn(
          "inline-flex cursor-help rounded-sm px-2 py-0.5 text-xs font-medium capitalize",
          incidentStatusStyles[status]
        )}
      >
        {status}
      </span>
    </SimpleTooltip>
  )
}

function IncidentDuration({ incident }: { incident: IncidentResponse }) {
  const isOngoing = getIncidentStatus(incident) === "ongoing"
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!isOngoing) {
      return
    }

    const interval = window.setInterval(() => {
      setNow(Date.now())
    }, 30_000)

    return () => {
      window.clearInterval(interval)
    }
  }, [isOngoing])

  const durationSeconds = Math.max(
    0,
    Math.floor(getIncidentDurationMs(incident, now) / 1000)
  )

  return <span>{formatDurationSeconds(durationSeconds)}</span>
}

function TimestampCell({ iso }: { iso: string }) {
  return (
    <SimpleTooltip content={formatDateWithRelative(iso)}>
      <span className="cursor-help">{formatDate(iso)}</span>
    </SimpleTooltip>
  )
}

function ServerIncidentsView({
  page,
  pagination,
  onPageChange,
  onPageSizeChange,
}: ServerIncidentsViewProps) {
  const columns = useMemo<ColumnDef<IncidentResponse>[]>(
    () => [
      {
        id: "status",
        header: "Status",
        cell: ({ row }) => <IncidentStatusBadge incident={row.original} />,
      },
      {
        id: "startedAt",
        header: "Started",
        cell: ({ row }) => <TimestampCell iso={row.original.startedAt} />,
      },
      {
        id: "resolvedAt",
        header: "Resolved",
        cell: ({ row }) =>
          row.original.resolvedAt ? (
            <TimestampCell iso={row.original.resolvedAt} />
          ) : (
            <span className="text-muted-foreground">—</span>
          ),
      },
      {
        id: "duration",
        header: "Duration",
        cell: ({ row }) => <IncidentDuration incident={row.original} />,
      },
    ],
    []
  )

  const table = useReactTable({
    data: page.items,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  if (page.totalItems === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        No outages recorded yet. Incidents are created automatically when the
        agent stops reporting heartbeats.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <DataTable table={table} />

      <Pagination
        page={page}
        currentPage={pagination.page}
        pageSize={pagination.count}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        itemLabel="incident"
      />
    </div>
  )
}

export { ServerIncidentsView }
