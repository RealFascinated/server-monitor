import type { ColumnDef } from "@tanstack/react-table"

import { RoleTag } from "@/components/server/role-tag"
import { TableHeaderTooltip } from "@/components/simple-tooltip"
import { TimestampCell } from "@/components/timestamp-cell"
import type { ServerRole } from "@/lib/api/user/servers"
import { formatDateWithRelative } from "@/lib/formatter"
import { INVITE_EXPIRY_TOOLTIP } from "@/lib/tooltips/copy"

type InviteTimestampRow = {
  role: ServerRole
  createdAt: string
  expiresAt: string
}

type BuildInviteTableColumnsOptions<T extends InviteTimestampRow> = {
  roleTooltip: string
  sentTooltip: string
  expiresTooltip?: string
  actionsCell: NonNullable<ColumnDef<T>["cell"]>
}

function buildInviteTableColumns<T extends InviteTimestampRow>({
  roleTooltip,
  sentTooltip,
  expiresTooltip = "Pending invites stop working after this time.",
  actionsCell,
}: BuildInviteTableColumnsOptions<T>): ColumnDef<T>[] {
  return [
    {
      accessorKey: "role",
      header: () => <TableHeaderTooltip label="Role" tooltip={roleTooltip} />,
      cell: ({ row }) => <RoleTag role={row.original.role} />,
    },
    {
      accessorKey: "createdAt",
      header: () => <TableHeaderTooltip label="Sent" tooltip={sentTooltip} />,
      meta: { className: "text-muted-foreground" },
      cell: ({ row }) => <TimestampCell iso={row.original.createdAt} />,
    },
    {
      accessorKey: "expiresAt",
      header: () => (
        <TableHeaderTooltip label="Expires" tooltip={expiresTooltip} />
      ),
      meta: { className: "text-muted-foreground" },
      cell: ({ row }) => (
        <TimestampCell
          iso={row.original.expiresAt}
          tooltip={`${INVITE_EXPIRY_TOOLTIP} ${formatDateWithRelative(row.original.expiresAt)}`}
        />
      ),
    },
    {
      id: "actions",
      enableSorting: false,
      header: () => <span className="sr-only">Actions</span>,
      meta: { className: "w-0" },
      cell: actionsCell,
    },
  ]
}

export { buildInviteTableColumns }
export type { InviteTimestampRow }
