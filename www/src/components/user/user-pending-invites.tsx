import {
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import type { ColumnDef, SortingState } from "@tanstack/react-table"
import { useState } from "react"
import { buildInviteTableColumns } from "@/components/invite-table-columns"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import { useAcceptServerInvite } from "@/hooks/use-accept-server-invite"
import { useUserInvites } from "@/hooks/use-user-invites"
import { acceptServerInviteById } from "@/lib/api/user/invites"
import type { UserPendingInvite } from "@/lib/api/user/invites"
import { SERVER_ROLE_TOOLTIPS } from "@/lib/tooltips/copy"

function AcceptInviteButton({
  inviteId,
  serverName,
}: {
  inviteId: number
  serverName: string
}) {
  const mutation = useAcceptServerInvite({
    mutationFn: () => acceptServerInviteById(inviteId),
    errorServerName: serverName,
  })

  return (
    <Button
      type="button"
      variant="highlighted"
      size="sm"
      disabled={mutation.isPending}
      onClick={() => {
        mutation.mutate()
      }}
    >
      {mutation.isPending ? <Spinner /> : null}
      Accept
    </Button>
  )
}

const inviteColumns: ColumnDef<UserPendingInvite>[] = [
  {
    accessorKey: "serverName",
    header: "Server",
    meta: { className: "font-medium" },
    cell: ({ row }) => row.original.serverName,
  },
  {
    accessorKey: "invitedByEmail",
    header: "Invited by",
    meta: { className: "text-muted-foreground" },
    cell: ({ row }) => row.original.invitedByEmail,
  },
  ...buildInviteTableColumns<UserPendingInvite>({
    roleTooltip: SERVER_ROLE_TOOLTIPS.VIEWER,
    sentTooltip: "When the invite was sent to your account.",
    expiresTooltip: "Accept the invite before this time.",
    actionsCell: ({ row }) => (
      <AcceptInviteButton
        inviteId={row.original.inviteId}
        serverName={row.original.serverName}
      />
    ),
  }),
]

function UserPendingInvites() {
  const [sorting, setSorting] = useState<SortingState>([])
  const { data: invites = [], isPending, error } = useUserInvites()

  const table = useReactTable({
    data: invites,
    columns: inviteColumns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getRowId: (row) => String(row.inviteId),
    state: { sorting },
    onSortingChange: setSorting,
  })

  return (
    <QueryStatusShell
      error={error}
      isPending={isPending}
      loadingMessage="Loading invites…"
      fallbackMessage="Failed to load invites"
      fallbackTitle="Could not load invites"
      className="flex flex-col gap-3"
    >
      {invites.length === 0 ? (
        <p className="text-muted-foreground">No pending invites.</p>
      ) : null}

      {invites.length > 0 ? <DataTable table={table} /> : null}
    </QueryStatusShell>
  )
}

export { UserPendingInvites }
