import { queryOptions } from "@tanstack/react-query"

import { getServerAccess } from "@/lib/api/user/access"

export function serverAccessQueryKey(serverId: number) {
  return ["user", "servers", serverId, "access"] as const
}

export function serverAccessQueryOptions(serverId: number) {
  return queryOptions({
    queryKey: serverAccessQueryKey(serverId),
    queryFn: () => getServerAccess(serverId),
  })
}
