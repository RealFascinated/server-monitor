import type { QueryClient } from "@tanstack/react-query"
import { queryOptions } from "@tanstack/react-query"

import {
  getServerStatus,
  getUserServer,
  getUserServers,
} from "@/lib/api/user/servers"
import type { ServerResponse } from "@/lib/api/user/servers"

const SERVER_STATUS_POLL_MS = 3_000

export const userServersQueryKey = ["user", "servers"] as const

export function userServerQueryKey(serverId: number) {
  return [...userServersQueryKey, serverId] as const
}

export function userServersQueryOptions() {
  return queryOptions({
    queryKey: userServersQueryKey,
    queryFn: getUserServers,
  })
}

export function userServerQueryOptions(serverId: number) {
  return queryOptions({
    queryKey: userServerQueryKey(serverId),
    queryFn: () => getUserServer(serverId),
  })
}

export async function resolveUserServer(
  queryClient: QueryClient,
  serverId: number
): Promise<ServerResponse> {
  const cached = queryClient.getQueryData<ServerResponse>(
    userServerQueryKey(serverId)
  )
  if (cached) {
    return cached
  }

  const servers =
    queryClient.getQueryData<ServerResponse[]>(userServersQueryKey)
  const fromList = servers?.find((server) => server.serverId === serverId)
  if (fromList) {
    queryClient.setQueryData(userServerQueryKey(serverId), fromList)
    return fromList
  }

  return queryClient.ensureQueryData(userServerQueryOptions(serverId))
}

export function resolveUserServerFromCache(
  queryClient: QueryClient,
  serverId: number
): ServerResponse | undefined {
  const cached = queryClient.getQueryData<ServerResponse>(
    userServerQueryKey(serverId)
  )
  if (cached) {
    return cached
  }

  const servers =
    queryClient.getQueryData<ServerResponse[]>(userServersQueryKey)
  const fromList = servers?.find((server) => server.serverId === serverId)
  if (fromList) {
    queryClient.setQueryData(userServerQueryKey(serverId), fromList)
    return fromList
  }

  void queryClient.prefetchQuery(userServerQueryOptions(serverId))
  return undefined
}

export function userServerStatusQueryKey(serverId: number) {
  return [...userServerQueryKey(serverId), "status"] as const
}

export function userServerStatusQueryOptions(serverId: number) {
  return queryOptions({
    queryKey: userServerStatusQueryKey(serverId),
    queryFn: () => getServerStatus(serverId),
    refetchInterval: (query) =>
      query.state.data?.hasMetrics ? false : SERVER_STATUS_POLL_MS,
  })
}

export function serversById(
  servers: ServerResponse[]
): Record<number, ServerResponse> {
  return Object.fromEntries(servers.map((server) => [server.serverId, server]))
}

export function updateServerInCaches(
  queryClient: QueryClient,
  server: ServerResponse
) {
  queryClient.setQueryData(userServerQueryKey(server.serverId), server)
  queryClient.setQueryData<ServerResponse[]>(
    userServersQueryKey,
    (current) =>
      current?.map((entry) =>
        entry.serverId === server.serverId ? server : entry
      ) ?? [server]
  )
}

export function removeServerFromCaches(
  queryClient: QueryClient,
  serverId: number
) {
  queryClient.removeQueries({ queryKey: userServerQueryKey(serverId) })
  queryClient.setQueryData<ServerResponse[]>(userServersQueryKey, (current) =>
    current?.filter((entry) => entry.serverId !== serverId)
  )
}

export function updateServerFolderNameInCache(
  queryClient: QueryClient,
  serverId: number,
  folderName: string | null
) {
  const update = (server: ServerResponse) =>
    server.serverId === serverId ? { ...server, folderName } : server

  queryClient.setQueryData<ServerResponse[]>(userServersQueryKey, (current) =>
    current?.map(update)
  )
  queryClient.setQueryData<ServerResponse>(
    userServerQueryKey(serverId),
    (current) => (current ? { ...current, folderName } : current)
  )
}
