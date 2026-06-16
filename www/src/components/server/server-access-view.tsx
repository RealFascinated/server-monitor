import { useMutation, useQueryClient } from "@tanstack/react-query"
import {
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import type { ColumnDef, SortingState } from "@tanstack/react-table"
import { Trash2 } from "lucide-react"
import { useMemo, useState } from "react"

import { ConfirmDialog } from "@/components/confirm-dialog"
import { SimpleTooltip, TableHeaderTooltip } from "@/components/simple-tooltip"
import { InviteMemberDialog } from "@/components/server/invite-member-dialog"
import { Button } from "@/components/ui/button"
import { DataTable } from "@/components/ui/data-table"
import { removeServerMember, revokeServerInvite } from "@/lib/api/user/access"
import { serverAccessQueryKey } from "@/lib/api/user/access.queries"
import type {
  PendingServerInvite,
  ServerAccessListResponse,
} from "@/lib/api/user/access"
import type { ServerRole } from "@/lib/api/user/servers"
import { formatDate, formatDateWithRelative } from "@/lib/formatter"
import {
  INVITE_EXPIRY_TOOLTIP,
  SERVER_ROLE_TOOLTIPS,
} from "@/lib/tooltips/copy"
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

function formatRole(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase()
}

function RoleTag({ role }: { role: ServerRole | "OWNER" }) {
  const tooltip = SERVER_ROLE_TOOLTIPS[role]

  return (
    <SimpleTooltip content={tooltip}>
      <span className="cursor-help bg-muted px-2 py-1 text-xs font-bold text-muted-foreground">
        {formatRole(role)}
      </span>
    </SimpleTooltip>
  )
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
            <SimpleTooltip
              content={formatDateWithRelative(row.original.joinedAt)}
            >
              <span className="cursor-help">
                {formatDate(row.original.joinedAt)}
              </span>
            </SimpleTooltip>
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
      {
        accessorKey: "role",
        header: () => (
          <TableHeaderTooltip
            label="Role"
            tooltip="Viewer invites grant read-only access to this server's metrics."
          />
        ),
        cell: ({ row }) => <RoleTag role={row.original.role} />,
      },
      {
        accessorKey: "createdAt",
        header: () => (
          <TableHeaderTooltip
            label="Sent"
            tooltip="When the invite was created."
          />
        ),
        meta: { className: "text-muted-foreground" },
        cell: ({ row }) => (
          <SimpleTooltip
            content={formatDateWithRelative(row.original.createdAt)}
          >
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
            tooltip="Pending invites stop working after this time."
          />
        ),
        meta: { className: "text-muted-foreground" },
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
          <RevokeInviteButton
            serverId={serverId}
            inviteId={row.original.inviteId}
            inviteEmail={row.original.email}
          />
        ),
      },
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
