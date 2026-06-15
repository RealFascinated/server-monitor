import { ServerDetailHeader } from "@/components/server/server-detail-header"
import type { ServerResponse } from "@/lib/api/user/servers"

type ServerIncidentsHeaderProps = {
  server: ServerResponse | undefined
  serverId: number
}

function ServerIncidentsHeader({
  server,
  serverId,
}: ServerIncidentsHeaderProps) {
  return (
    <ServerDetailHeader
      server={server}
      serverId={serverId}
      breadcrumbTail={{ label: "Incidents", current: true }}
    />
  )
}

export { ServerIncidentsHeader }
