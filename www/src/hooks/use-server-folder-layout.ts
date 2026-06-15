import { useMemo } from "react"

import { useUserServers } from "@/hooks/use-user-servers"
import type { ServerFolderLayout } from "@/lib/servers/group-by-folder"
import { partitionServerIdsByFolder } from "@/lib/servers/group-by-folder"

export function useServerFolderLayout(): ServerFolderLayout {
  const { data: servers = [] } = useUserServers()

  return useMemo(
    () =>
      partitionServerIdsByFolder(
        servers.map((server) => server.serverId),
        (serverId) => servers.find((server) => server.serverId === serverId)!,
        { sortServers: true }
      ),
    [servers]
  )
}

export function useServerIds() {
  const { data: servers = [] } = useUserServers()
  return useMemo(() => servers.map((server) => server.serverId), [servers])
}
