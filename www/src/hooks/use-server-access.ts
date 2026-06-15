import { useQuery } from "@tanstack/react-query"

import { serverAccessQueryOptions } from "@/lib/api/user/access.queries"

export function useServerAccess(serverId: number, enabled = true) {
  return useQuery({
    ...serverAccessQueryOptions(serverId),
    enabled,
  })
}
