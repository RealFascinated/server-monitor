import { ServerDetailHeader } from "@/components/server/server-detail-header"
import type { ServerResponse } from "@/lib/api/user/servers"

type ServerSettingsHeaderProps = {
  server: ServerResponse | undefined
  serverId: number
}

function ServerSettingsHeader({ server, serverId }: ServerSettingsHeaderProps) {
  return (
    <ServerDetailHeader
      server={server}
      serverId={serverId}
      breadcrumbTail={{ label: "Settings", current: true }}
    />
  )
}

export { ServerSettingsHeader }
