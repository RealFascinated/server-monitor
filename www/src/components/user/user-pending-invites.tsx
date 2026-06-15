import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import type { ColumnDef, SortingState } from "@tanstack/react-table"
import { useNavigate } from "@tanstack/react-router"
import { useState } from "react"
import { AsyncContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
import { SimpleTooltip, TableHeaderTooltip } from "@/components/simple-tooltip"
import { Spinner } from "@/components/spinner"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import { useUserInvites } from "@/hooks/use-user-invites"
import { acceptServerInviteById } from "@/lib/api/user/invites"
import type { UserPendingInvite } from "@/lib/api/user/invites"
import { defaultMetricRangeSearch } from "@/lib/metrics/default-range"
import { userInvitesQueryKey } from "@/lib/api/user/invites.queries"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { formatDate, formatDateWithRelative } from "@/lib/formatter"
import { toastMutationError } from "@/lib/toast"
import {
  INVITE_EXPIRY_TOOLTIP,
  SERVER_ROLE_TOOLTIPS,
} from "@/lib/tooltips/copy"

function formatRole(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase()
}

function AcceptInviteButton({
  inviteId,
  serverName,
}: {
  inviteId: number
  serverName: string
}) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => acceptServerInviteById(inviteId),
    onSuccess: async (member) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: userServersQueryKey }),
        queryClient.invalidateQueries({ queryKey: userInvitesQueryKey }),
      ])
      await navigate({
        to: "/servers/$serverId",
        params: { serverId: String(member.serverId) },
        search: defaultMetricRangeSearch(),
      })
    },
    onError: (mutationError) => {
      toastMutationError(
        `Could not accept invite to ${serverName}`,
        mutationError,
        "Failed to accept invite"
      )
    },
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

const columns: ColumnDef<UserPendingInvite>[] = [
  {
    accessorKey: "serverName",
    header: "Server",
    meta: { className: "font-medium" },
    cell: ({ row }) => row.original.serverName,
  },
  {
    accessorKey: "invitedByEmail",
    header: "Invited by",
    meta: { className: "text-neutral-500" },
    cell: ({ row }) => row.original.invitedByEmail,
  },
  {
    accessorKey: "role",
    header: () => (
      <TableHeaderTooltip label="Role" tooltip={SERVER_ROLE_TOOLTIPS.VIEWER} />
    ),
    cell: ({ row }) => (
      <SimpleTooltip content={SERVER_ROLE_TOOLTIPS.VIEWER}>
        <span className="cursor-help">{formatRole(row.original.role)}</span>
      </SimpleTooltip>
    ),
  },
  {
    accessorKey: "createdAt",
    header: () => (
      <TableHeaderTooltip
        label="Received"
        tooltip="When the invite was sent to your account."
      />
    ),
    meta: { className: "text-neutral-500" },
    cell: ({ row }) => (
      <SimpleTooltip content={formatDateWithRelative(row.original.createdAt)}>
        <span className="cursor-help">
          {formatDate(row.original.createdAt)}
        </span>
      </SimpleTooltip>
    ),
  },
  {
    accessorKey: "expiresAt",
    header: () => (
      <TableHeaderTooltip
        label="Expires"
        tooltip="Accept the invite before this time."
      />
    ),
    meta: { className: "text-neutral-500" },
    cell: ({ row }) => (
      <SimpleTooltip
        content={`${INVITE_EXPIRY_TOOLTIP} ${formatDateWithRelative(row.original.expiresAt)}`}
      >
        <span className="cursor-help">
          {formatDate(row.original.expiresAt)}
        </span>
      </SimpleTooltip>
    ),
  },
  {
    id: "actions",
    enableSorting: false,
    header: () => <span className="sr-only">Actions</span>,
    meta: { className: "w-0" },
    cell: ({ row }) => (
      <AcceptInviteButton
        inviteId={row.original.inviteId}
        serverName={row.original.serverName}
      />
    ),
  },
]

function UserPendingInvites() {
  const [sorting, setSorting] = useState<SortingState>([])
  const { data: invites = [], isPending, error } = useUserInvites()

  const table = useReactTable({
    data: invites,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getRowId: (row) => String(row.inviteId),
    state: { sorting },
    onSortingChange: setSorting,
  })

  const errorMessage = error instanceof Error ? error.message : null

  return (
    <div className="flex flex-col gap-3">
      {errorMessage ? (
        <Callout type="danger" title="Could not load invites">
          {errorMessage}
        </Callout>
      ) : null}

      {!errorMessage ? (
        <AsyncContent
          loading={isPending}
          loadingMessage="Loading invites…"
          className="flex flex-col gap-3"
        >
          {invites.length === 0 ? (
            <p className="text-neutral-500">No pending invites.</p>
          ) : null}

          {invites.length > 0 ? <DataTable table={table} /> : null}
        </AsyncContent>
      ) : null}
    </div>
  )
}

export { UserPendingInvites }
