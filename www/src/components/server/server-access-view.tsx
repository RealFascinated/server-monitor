import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import type { ColumnDef, SortingState } from "@tanstack/react-table"
import { Trash2 } from "lucide-react"
import { useMemo, useState } from "react"

import { buildInviteTableColumns } from "@/components/invite-table-columns"
import { ConfirmDialog } from "@/components/confirm-dialog"
import { TableHeaderTooltip } from "@/components/simple-tooltip"
import { InviteMemberDialog } from "@/components/server/invite-member-dialog"
import { RoleTag } from "@/components/server/role-tag"
import { TimestampCell } from "@/components/timestamp-cell"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import { removeServerMember, revokeServerInvite } from "@/lib/api/user/access"
import { serverAccessQueryKey } from "@/lib/api/user/access.queries"
import type {
  PendingServerInvite,
  ServerAccessListResponse,
} from "@/lib/api/user/access"
import type { ServerRole } from "@/lib/api/user/servers"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type ServerAccessViewProps = {
  serverId: number
  access: ServerAccessListResponse
  canManage: boolean
}

type AccessMemberRow = {
  id: number
  email: string
  role: ServerRole | "OWNER"
  joinedAt: string | null
  memberUserId: number | null
}

function RemoveMemberButton({
  serverId,
  memberUserId,
  memberEmail,
}: {
  serverId: number
  memberUserId: number
  memberEmail: string
}) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => removeServerMember(serverId, memberUserId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: serverAccessQueryKey(serverId),
      })
      toastSuccess("Member removed")
    },
    onError: (mutationError) => {
      toastMutationError(
        "Could not remove member",
        mutationError,
        "Failed to remove member"
      )
    },
  })

  return (
    <ConfirmDialog
      trigger={
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          className="text-muted-foreground hover:bg-transparent hover:text-red-600 dark:text-muted-foreground dark:hover:bg-transparent dark:hover:text-red-400"
          aria-label={`Remove ${memberEmail}`}
        >
          <Trash2 className="size-4" />
        </Button>
      }
      title="Remove member"
      triggerTooltip="Remove member"
      description={
        <>
          Remove <span className="font-bold">{memberEmail}</span> from this
          server? They will lose access immediately.
        </>
      }
      confirmLabel="Remove"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

function RevokeInviteButton({
  serverId,
  inviteId,
  inviteEmail,
}: {
  serverId: number
  inviteId: number
  inviteEmail: string
}) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => revokeServerInvite(serverId, inviteId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: serverAccessQueryKey(serverId),
      })
      toastSuccess("Invite revoked")
    },
    onError: (mutationError) => {
      toastMutationError(
        "Could not revoke invite",
        mutationError,
        "Failed to revoke invite"
      )
    },
  })

  return (
    <ConfirmDialog
      trigger={
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          className="text-muted-foreground hover:bg-transparent hover:text-red-600 dark:text-muted-foreground dark:hover:bg-transparent dark:hover:text-red-400"
          aria-label={`Revoke invite for ${inviteEmail}`}
        >
          <Trash2 className="size-4" />
        </Button>
      }
      title="Revoke invite"
      triggerTooltip="Revoke invite"
      description={
        <>
          Revoke the pending invite for{" "}
          <span className="font-bold">{inviteEmail}</span>?
        </>
      }
      confirmLabel="Revoke"
      confirmVariant="destructive"
      onConfirm={async () => {
        await mutation.mutateAsync()
      }}
    />
  )
}

function ServerAccessView({
  serverId,
  access,
  canManage,
}: ServerAccessViewProps) {
  const [memberSorting, setMemberSorting] = useState<SortingState>([])
  const [inviteSorting, setInviteSorting] = useState<SortingState>([])

  const memberRows = useMemo<AccessMemberRow[]>(
    () => [
      {
        id: access.owner.id,
        email: access.owner.email,
        role: "OWNER",
        joinedAt: null,
        memberUserId: null,
      },
      ...access.members.map((member) => ({
        id: member.userId,
        email: member.email,
        role: member.role,
        joinedAt: member.joinedAt,
        memberUserId: member.userId,
      })),
    ],
    [access]
  )

  const memberColumns = useMemo<ColumnDef<AccessMemberRow>[]>(() => {
    const baseColumns: ColumnDef<AccessMemberRow>[] = [
      {
        accessorKey: "email",
        header: "Email",
        meta: { className: "font-bold" },
        cell: ({ row }) => row.original.email,
      },
      {
        accessorKey: "role",
        header: () => (
          <TableHeaderTooltip
            label="Role"
            tooltip="Owner has full control. Viewers can only read metrics."
          />
        ),
        cell: ({ row }) => <RoleTag role={row.original.role} />,
      },
      {
        accessorKey: "joinedAt",
        header: () => (
          <TableHeaderTooltip
            label="Joined"
            tooltip="When this member accepted access to the server."
          />
        ),
        meta: { className: "text-muted-foreground" },
        cell: ({ row }) =>
          row.original.joinedAt ? (
            <TimestampCell iso={row.original.joinedAt} />
          ) : (
            "—"
          ),
      },
    ]

    if (!canManage) {
      return baseColumns
    }

    return [
      ...baseColumns,
      {
        id: "actions",
        enableSorting: false,
        header: () => <span className="sr-only">Actions</span>,
        meta: { className: "w-0" },
        cell: ({ row }) => {
          if (row.original.memberUserId === null) {
            return <span className="inline-flex size-7" aria-hidden />
          }

          return (
            <RemoveMemberButton
              serverId={serverId}
              memberUserId={row.original.memberUserId}
              memberEmail={row.original.email}
            />
          )
        },
      },
    ]
  }, [canManage, serverId])

  const pendingInviteColumns = useMemo<ColumnDef<PendingServerInvite>[]>(
    () => [
      {
        accessorKey: "email",
        header: "Email",
        meta: { className: "font-bold" },
        cell: ({ row }) => row.original.email,
      },
      ...buildInviteTableColumns<PendingServerInvite>({
        roleTooltip:
          "Viewer invites grant read-only access to this server's metrics.",
        sentTooltip: "When the invite was created.",
        actionsCell: ({ row }) => (
          <RevokeInviteButton
            serverId={serverId}
            inviteId={row.original.inviteId}
            inviteEmail={row.original.email}
          />
        ),
      }),
    ],
    [serverId]
  )

  const membersTable = useReactTable({
    data: memberRows,
    columns: memberColumns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getRowId: (row) => String(row.id),
    state: { sorting: memberSorting },
    onSortingChange: setMemberSorting,
  })

  const pendingInvitesTable = useReactTable({
    data: access.pendingInvites,
    columns: pendingInviteColumns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getRowId: (row) => String(row.inviteId),
    state: { sorting: inviteSorting },
    onSortingChange: setInviteSorting,
  })

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between gap-4">
          <h3 className="text-lg font-bold text-foreground">Members</h3>
          {canManage ? <InviteMemberDialog serverId={serverId} /> : null}
        </div>

        <DataTable table={membersTable} />
      </div>

      {canManage ? (
        <div className="flex flex-col gap-3">
          <h3 className="text-lg font-bold text-foreground">Pending invites</h3>

          {access.pendingInvites.length === 0 ? (
            <p className="text-muted-foreground">No pending invites.</p>
          ) : (
            <DataTable table={pendingInvitesTable} />
          )}
        </div>
      ) : null}
    </div>
  )
}

export { ServerAccessView }
