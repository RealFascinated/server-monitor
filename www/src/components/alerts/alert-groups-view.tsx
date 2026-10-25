import {
  getCoreRowModel,
  useLegacyTable as useReactTable,
} from "@tanstack/react-table/legacy"
import type { LegacyColumnDef as ColumnDef } from "@tanstack/react-table/legacy"
import { flexRender } from "@tanstack/react-table"
import { Link, useNavigate } from "@tanstack/react-router"
import { FolderKanban, Pencil, Trash2 } from "lucide-react"
import { useMemo } from "react"

import { AlertOverviewCards } from "@/components/alerts/alert-overview-cards"
import { GroupDestination } from "@/components/alerts/group-destination"
import { GroupDialog } from "@/components/alerts/group-dialog"
import { GroupEnabledToggle } from "@/components/alerts/group-enabled-toggle"
import { Callout } from "@/components/callout"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { EmptyState } from "@/components/empty-state"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import { TableCell, TableRow } from "@/components/ui/table"
import { useAlertGroups, useDeleteAlertGroup } from "@/hooks/use-alert-groups"
import { useUserServers } from "@/hooks/use-user-servers"
import { computeAlertOverview } from "@/lib/alerts/overview"
import type { AlertGroup } from "@/lib/api/user/alerts"
import { cn } from "@/lib/utils"

const MAX_SERVER_CHIPS = 3

function ServerChips({ group }: { group: AlertGroup }) {
  if (group.servers.length === 0) {
    return <span className="text-muted-foreground">No servers</span>
  }

  const shown = group.servers.slice(0, MAX_SERVER_CHIPS)
  const hidden = group.servers.length - shown.length

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {shown.map((server) => (
        <span
          key={server.id}
          className="rounded-full border border-border bg-background px-2 py-0.5 text-xs"
        >
          {server.name}
        </span>
      ))}
      {hidden > 0 ? (
        <span className="text-xs text-muted-foreground">+{hidden} more</span>
      ) : null}
    </div>
  )
}

function GroupStatus({ group }: { group: AlertGroup }) {
  if (!group.enabled) {
    return <span className="text-muted-foreground">Disabled</span>
  }
  if (group.ruleCount === 0) {
    return <span className="text-muted-foreground">No alerts</span>
  }
  const firing = group.firingCount > 0

  return (
    <span
      className={cn(
        "inline-flex rounded-sm px-2 py-0.5 text-xs font-medium",
        firing
          ? "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300"
          : "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300"
      )}
    >
      {firing ? `${group.firingCount} firing` : "Healthy"}
    </span>
  )
}

function GroupActions({ group }: { group: AlertGroup }) {
  const remove = useDeleteAlertGroup()

  return (
    <div
      className="flex items-center justify-end gap-1"
      onClick={(event) => event.stopPropagation()}
    >
      <GroupEnabledToggle group={group} />
      <GroupDialog
        group={group}
        trigger={
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={`Edit ${group.name}`}
          >
            <Pencil />
          </Button>
        }
      />
      {group.defaultGroup ? null : (
        <ConfirmDialog
          trigger={
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={`Delete ${group.name}`}
            >
              <Trash2 />
            </Button>
          }
          title={`Delete ${group.name}?`}
          description={`This also deletes its ${group.ruleCount === 1 ? "1 alert" : `${group.ruleCount} alerts`}. The servers themselves are not affected.`}
          confirmLabel="Delete"
          confirmVariant="destructive"
          onConfirm={() => remove.mutateAsync(group.id)}
        />
      )}
    </div>
  )
}

function GroupsTable({ groups }: { groups: AlertGroup[] }) {
  const navigate = useNavigate()

  const columns = useMemo<ColumnDef<AlertGroup>[]>(
    () => [
      {
        id: "name",
        header: "Group",
        cell: ({ row }) => (
          <div className="flex items-center gap-2">
            <Link
              to="/alerts/groups/$groupId"
              params={{ groupId: String(row.original.id) }}
              className="font-medium text-foreground hover:text-brand"
              onClick={(event) => event.stopPropagation()}
            >
              {row.original.name}
            </Link>
          </div>
        ),
      },
      {
        id: "servers",
        header: "Servers",
        cell: ({ row }) => <ServerChips group={row.original} />,
      },
      {
        id: "notifies",
        header: "Notifies",
        cell: ({ row }) => <GroupDestination group={row.original} />,
      },
      {
        id: "alerts",
        header: "Alerts",
        cell: ({ row }) => row.original.ruleCount,
      },
      {
        id: "status",
        header: "Status",
        cell: ({ row }) => <GroupStatus group={row.original} />,
      },
      {
        id: "actions",
        header: () => <span className="sr-only">Actions</span>,
        cell: ({ row }) => <GroupActions group={row.original} />,
      },
    ],
    []
  )

  const table = useReactTable({
    data: groups,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  return (
    <DataTable
      table={table}
      renderRow={(row) => (
        <TableRow
          key={row.id}
          className="cursor-pointer"
          onClick={() =>
            navigate({
              to: "/alerts/groups/$groupId",
              params: { groupId: String(row.original.id) },
            })
          }
        >
          {row.getVisibleCells().map((cell) => (
            <TableCell key={cell.id}>
              {flexRender(cell.column.columnDef.cell, cell.getContext())}
            </TableCell>
          ))}
        </TableRow>
      )}
    />
  )
}

function AlertGroupsView() {
  const groups = useAlertGroups()
  const servers = useUserServers()

  const overview = useMemo(
    () => computeAlertOverview(groups.data ?? [], servers.data ?? []),
    [groups.data, servers.data]
  )

  return (
    <QueryStatusShell
      error={groups.error}
      isPending={groups.isPending}
      loadingMessage="Loading groups…"
      fallbackMessage="Could not load alert groups."
      fallbackTitle="Groups unavailable"
    >
      <div className="flex flex-col gap-6">
        <AlertOverviewCards
          overview={overview}
          serverCount={servers.data?.length ?? 0}
        />

        {overview.uncoveredServers.length > 0 ? (
          <Callout type="warning" title="Some servers have no alerts">
            {overview.uncoveredServers
              .map((server) => server.serverName)
              .join(", ")}{" "}
            {overview.uncoveredServers.length === 1 ? "is" : "are"} not in any
            enabled alert group. Add{" "}
            {overview.uncoveredServers.length === 1 ? "it" : "them"} to a group,
            or enable one, to be notified when something goes wrong.
          </Callout>
        ) : null}

        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-muted-foreground">
              Put servers in a group and every alert on the group applies to all
              of them.
            </p>
            <GroupDialog />
          </div>

          {groups.data && groups.data.length > 0 ? (
            <GroupsTable groups={groups.data} />
          ) : (
            <EmptyState
              icon={FolderKanban}
              title="No alert groups"
              description="Create a group, add servers to it, then add the alerts those servers should share."
              action={<GroupDialog />}
            />
          )}
        </div>
      </div>
    </QueryStatusShell>
  )
}

export { AlertGroupsView }
