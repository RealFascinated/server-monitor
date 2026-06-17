import { useQueryClient } from "@tanstack/react-query"

import { serverAccessQueryKey } from "@/lib/api/user/access.queries"
import { removeServerFromCaches } from "@/lib/api/user/servers.queries"

export function useRemoveServerFromUser(serverId: number) {
  const queryClient = useQueryClient()

  function removeFromCaches() {
    removeServerFromCaches(queryClient, serverId)
    queryClient.removeQueries({ queryKey: serverAccessQueryKey(serverId) })
  }

  return { removeFromCaches }
}
