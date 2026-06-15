import { useQuery } from "@tanstack/react-query"

import {
  userServerQueryOptions,
  userServersQueryOptions,
} from "@/lib/api/user/servers.queries"

export function useUserServer(serverId: number) {
  const listQuery = useQuery(userServersQueryOptions())
  const serverFromList = listQuery.data?.find(
    (server) => server.serverId === serverId
  )

  const detailQuery = useQuery({
    ...userServerQueryOptions(serverId),
    enabled: listQuery.isSuccess && serverFromList === undefined,
  })

  const data = serverFromList ?? detailQuery.data
  const isPending =
    listQuery.isPending ||
    (listQuery.isSuccess && data === undefined && detailQuery.isPending)

  return {
    data,
    isLoading: isPending,
    isPending,
    error: listQuery.error ?? detailQuery.error,
  }
}
