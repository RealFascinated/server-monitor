import { useNavigate } from "@tanstack/react-router"

import { ServerAccessView } from "@/components/server/server-access-view"
import { ServerIngestTokenSection } from "@/components/server/server-ingest-token-section"
import { SettingsPageContent } from "@/components/settings/settings-page-content"
import { SettingsSectionHeader } from "@/components/settings/settings-section-header"
import { DeleteServerButton } from "@/components/user/delete-server-button"
import { LeaveServerButton } from "@/components/user/leave-server-button"
import { RenameServerForm } from "@/components/user/rename-server-form"
import type { ServerAccessListResponse } from "@/lib/api/user/access"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import type { ServerResponse } from "@/lib/api/user/servers"

type ServerSettingsViewProps = {
  serverId: number
  server: ServerResponse
  access: ServerAccessListResponse | undefined
}

function ServerSettingsView({
  serverId,
  server,
  access,
}: ServerSettingsViewProps) {
  const navigate = useNavigate()
  const { permissions } = server
  const canRename = hasPermission(permissions, ServerPermission.RENAME_SERVER)
  const canRotateIngestToken = hasPermission(
    permissions,
    ServerPermission.ROTATE_INGEST_TOKEN
  )
  const canListMembers = hasPermission(
    permissions,
    ServerPermission.LIST_MEMBERS
  )
  const canManageMembers = hasPermission(
    permissions,
    ServerPermission.INVITE_MEMBERS
  )
  const canDelete = hasPermission(permissions, ServerPermission.DELETE_SERVER)
  const canLeave = hasPermission(permissions, ServerPermission.LEAVE_SERVER)

  return (
    <SettingsPageContent>
      {canRename ? (
        <section className="flex flex-col gap-3">
          <SettingsSectionHeader
            title="General"
            description="Display name shown across the servers list and server pages."
          />
          <RenameServerForm
            serverId={serverId}
            currentName={server.serverName}
          />
        </section>
      ) : null}

      {canRotateIngestToken ? (
        <section className="flex flex-col gap-3">
          <SettingsSectionHeader
            title="Monitor Agent"
            description="Connection status, ingest token, and install instructions."
          />
          <ServerIngestTokenSection
            serverId={serverId}
            status={server.status}
            agentVersion={server.agentVersion}
          />
        </section>
      ) : null}

      {canListMembers && access ? (
        <section className="flex flex-col gap-3">
          <SettingsSectionHeader
            title="Access"
            description="Manage who can view this server's metrics."
          />
          <ServerAccessView
            serverId={serverId}
            access={access}
            canManage={canManageMembers}
          />
        </section>
      ) : null}

      {canDelete ? (
        <section className="flex flex-col gap-3">
          <SettingsSectionHeader
            title="Danger zone"
            description="Permanently delete this server and all stored metrics."
          />
          <div className="flex flex-wrap items-center justify-between gap-4">
            <p className="text-sm text-neutral-500">
              This action cannot be undone.
            </p>
            <DeleteServerButton
              serverId={serverId}
              serverName={server.serverName}
              variant="destructive"
              onDeleted={() => {
                void navigate({ to: "/" })
              }}
            />
          </div>
        </section>
      ) : null}

      {canLeave ? (
        <section className="flex flex-col gap-3">
          <SettingsSectionHeader
            title="Leave server"
            description="Remove your viewer access to this server."
          />
          <div className="flex flex-wrap items-center justify-between gap-4">
            <p className="text-sm text-neutral-500">
              You can rejoin if the owner sends a new invite.
            </p>
            <LeaveServerButton
              serverId={serverId}
              serverName={server.serverName}
              onLeft={() => {
                void navigate({ to: "/" })
              }}
            />
          </div>
        </section>
      ) : null}
    </SettingsPageContent>
  )
}

export { ServerSettingsView }
