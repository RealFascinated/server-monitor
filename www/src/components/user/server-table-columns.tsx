import { Link } from "@tanstack/react-router"
import type { ColumnDef } from "@tanstack/react-table"
import type { ReactNode } from "react"

import { DeleteServerButton } from "@/components/user/delete-server-button"
import { RenameServerDialog } from "@/components/user/rename-server-dialog"
import { AgentVersionLabel } from "@/components/server/agent-version-label"
import {
  CpuPercent,
  DiskPercent,
  MemoryPercent,
} from "@/components/server/usage-percent"
import { ServerStatusBadge } from "@/components/server/server-status-badge"
import { SimpleTooltip, TableHeaderTooltip } from "@/components/simple-tooltip"
import {
  formatDate,
  formatDateWithRelative,
  formatUptime,
  formatUptimeDetailed,
  formatUptimePercent30d,
} from "@/lib/formatter"
import type { ServerResponse } from "@/lib/api/user/servers"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import type { MetricTimeRange } from "@/lib/metrics/range"
import {
  pendingOnlyTooltip,
  SERVER_TABLE_COLUMN_TOOLTIPS,
} from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

export type ServerTableRow = {
  serverId: number
  server: ServerResponse
}

const unknownStatClassName = "text-neutral-500"

function renderUptime(server: ServerResponse): ReactNode {
  const formatted = formatUptime(server.uptimeSeconds)
  const detailed = formatUptimeDetailed(server.uptimeSeconds)
  const tooltip = detailed ?? pendingOnlyTooltip(server.status)
  const className = cn(server.uptimeSeconds == null && unknownStatClassName)

  if (!tooltip) {
    return <span className={className}>{formatted}</span>
  }

  return (
    <SimpleTooltip content={tooltip}>
      <span className={cn("cursor-help", className)}>{formatted}</span>
    </SimpleTooltip>
  )
}

function renderUptime30d(server: ServerResponse): ReactNode {
  const formatted = formatUptimePercent30d(server.uptimePercent30d)
  const tooltip = pendingOnlyTooltip(server.status)
  const className = cn(server.uptimePercent30d == null && unknownStatClassName)

  if (server.uptimePercent30d != null || !tooltip) {
    return <span className={className}>{formatted}</span>
  }

  return (
    <SimpleTooltip content={tooltip}>
      <span className={cn("cursor-help", className)}>{formatted}</span>
    </SimpleTooltip>
  )
}

export function getServerTableColumns(
  showActionsColumn: boolean,
  defaultRange: MetricTimeRange
): ColumnDef<ServerTableRow>[] {
  const baseColumns: ColumnDef<ServerTableRow>[] = [
    {
      accessorKey: "serverName",
      accessorFn: (row) => row.server.serverName,
      header: "Name",
      meta: {
        renderServer: (server) => (
          <span className="font-medium">
            <Link
              to="/servers/$serverId"
              params={{ serverId: String(server.serverId) }}
              search={{ range: defaultRange }}
              className="text-monitor hover:underline dark:text-warning"
            >
              {server.serverName}
            </Link>
          </span>
        ),
      },
    },
    {
      accessorKey: "status",
      accessorFn: (row) => row.server.status,
      header: () => (
        <TableHeaderTooltip
          label="Status"
          tooltip="Whether the Monitor Agent is reporting metrics for this server."
        />
      ),
      meta: {
        renderServer: (server) => <ServerStatusBadge status={server.status} />,
      },
    },
    {
      accessorKey: "uptimeSeconds",
      accessorFn: (row) => row.server.uptimeSeconds ?? null,
      header: () => (
        <TableHeaderTooltip
          label="Uptime"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.uptime}
        />
      ),
      meta: { renderServer: renderUptime },
    },
    {
      accessorKey: "uptimePercent30d",
      accessorFn: (row) => row.server.uptimePercent30d ?? null,
      header: () => (
        <TableHeaderTooltip
          label="Uptime (30d)"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.uptime30d}
        />
      ),
      meta: { renderServer: renderUptime30d },
    },
    {
      id: "cpu",
      accessorFn: (row) => row.server.cpu?.percent ?? null,
      header: () => (
        <TableHeaderTooltip
          label="CPU"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.cpu}
        />
      ),
      meta: {
        renderServer: (server) => (
          <CpuPercent cpu={server.cpu} status={server.status} />
        ),
      },
    },
    {
      id: "memory",
      accessorFn: (row) => {
        const { usage, max } = row.server.memory ?? {}
        return usage != null && max ? usage / max : null
      },
      header: () => (
        <TableHeaderTooltip
          label="Memory"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.memory}
        />
      ),
      meta: {
        renderServer: (server) => (
          <MemoryPercent
            usage={server.memory?.usage ?? null}
            max={server.memory?.max ?? null}
            status={server.status}
          />
        ),
      },
    },
    {
      id: "disk",
      accessorFn: (row) => {
        const { usage, max } = row.server.disk ?? {}
        return usage != null && max ? usage / max : null
      },
      header: () => (
        <TableHeaderTooltip
          label="Root Disk"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.rootDisk}
        />
      ),
      meta: {
        renderServer: (server) => (
          <DiskPercent
            usage={server.disk?.usage ?? null}
            max={server.disk?.max ?? null}
            status={server.status}
          />
        ),
      },
    },
    {
      accessorKey: "agentVersion",
      accessorFn: (row) => row.server.agentVersion ?? "",
      header: () => (
        <TableHeaderTooltip
          label="Agent"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.agent}
        />
      ),
      meta: {
        renderServer: (server) => (
          <AgentVersionLabel version={server.agentVersion} />
        ),
      },
    },
    {
      accessorKey: "createdAt",
      accessorFn: (row) => row.server.createdAt,
      header: () => (
        <TableHeaderTooltip
          label="Created"
          tooltip={SERVER_TABLE_COLUMN_TOOLTIPS.created}
        />
      ),
      meta: {
        className: "text-neutral-500",
        renderServer: (server) => (
          <SimpleTooltip content={formatDateWithRelative(server.createdAt)}>
            <span className="cursor-help">{formatDate(server.createdAt)}</span>
          </SimpleTooltip>
        ),
      },
    },
  ]

  if (!showActionsColumn) {
    return baseColumns
  }

  return [
    ...baseColumns,
    {
      id: "actions",
      enableSorting: false,
      header: () => <span className="sr-only">Actions</span>,
      meta: {
        className: "w-0",
        renderServer: (server) => {
          const canRename = hasPermission(
            server.permissions,
            ServerPermission.RENAME_SERVER
          )
          const canDelete = hasPermission(
            server.permissions,
            ServerPermission.DELETE_SERVER
          )

          if (!canRename && !canDelete) {
            return null
          }

          return (
            <div className="flex items-center">
              {canRename ? (
                <RenameServerDialog
                  serverId={server.serverId}
                  currentName={server.serverName}
                />
              ) : null}
              {canDelete ? (
                <DeleteServerButton
                  serverId={server.serverId}
                  serverName={server.serverName}
                />
              ) : null}
            </div>
          )
        },
      },
    },
  ]
}

export function filterServerIdsBySearch(
  serverIds: number[],
  searchQuery: string,
  servers: Record<number, ServerResponse>
): number[] {
  const search = searchQuery.trim().toLowerCase()
  if (!search) {
    return serverIds
  }

  return serverIds.filter((serverId) => {
    const server = servers[serverId]

    return (
      server.serverName.toLowerCase().includes(search) ||
      server.status.toLowerCase().includes(search) ||
      (server.agentVersion?.toLowerCase().includes(search) ?? false)
    )
  })
}
