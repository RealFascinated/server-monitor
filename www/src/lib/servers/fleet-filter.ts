import type { ServerResponse } from "@/lib/api/user/servers"
import { memoryUsagePercent } from "@/lib/formatter"
import { serverNeedsAttention } from "@/lib/servers/fleet-summary"

export const FLEET_FILTERS = [
  "all",
  "online",
  "offline",
  "pending",
  "attention",
] as const

export type FleetFilter = (typeof FLEET_FILTERS)[number]

export const SERVER_SORTS = [
  "attention",
  "custom",
  "name",
  "cpu",
  "memory",
] as const

export type ServerSort = (typeof SERVER_SORTS)[number]

export const SERVER_SORT_LABELS: Record<ServerSort, string> = {
  attention: "Needs attention first",
  custom: "My order",
  name: "Name (A-Z)",
  cpu: "CPU (high to low)",
  memory: "Memory (high to low)",
}

export const FLEET_FILTER_LABELS: Record<FleetFilter, string> = {
  all: "All",
  online: "Online",
  offline: "Offline",
  pending: "Pending",
  attention: "Needs attention",
}

export function serverMatchesFilter(
  server: ServerResponse,
  filter: FleetFilter
): boolean {
  switch (filter) {
    case "all":
      return true
    case "online":
      return server.status === "ONLINE"
    case "offline":
      return server.status === "OFFLINE"
    case "pending":
      return server.status === "PENDING"
    case "attention":
      return serverNeedsAttention(server)
  }
}

export function countByFleetFilter(
  servers: ServerResponse[]
): Record<FleetFilter, number> {
  const counts: Record<FleetFilter, number> = {
    all: servers.length,
    online: 0,
    offline: 0,
    pending: 0,
    attention: 0,
  }

  for (const server of servers) {
    if (server.status === "ONLINE") counts.online++
    if (server.status === "OFFLINE") counts.offline++
    if (server.status === "PENDING") counts.pending++
    if (serverNeedsAttention(server)) counts.attention++
  }

  return counts
}

function memoryPercent(server: ServerResponse): number {
  return (
    memoryUsagePercent(
      server.memory?.usage ?? null,
      server.memory?.max ?? null
    ) ?? -1
  )
}

function diskPercent(server: ServerResponse): number {
  return (
    memoryUsagePercent(server.disk?.usage ?? null, server.disk?.max ?? null) ??
    -1
  )
}

/** Higher scores are more urgent. Offline beats high usage beats healthy. */
function attentionScore(server: ServerResponse): number {
  if (server.status === "OFFLINE") {
    return 1000
  }

  if (serverNeedsAttention(server)) {
    return (
      100 +
      Math.max(server.cpu?.percent ?? 0, memoryPercent(server), diskPercent(server))
    )
  }

  return server.status === "ONLINE" ? 0 : -1
}

export function sortServerIds(
  serverIds: number[],
  servers: Record<number, ServerResponse>,
  sort: ServerSort
): number[] {
  if (sort === "custom") {
    return serverIds
  }

  const indexed = serverIds.map((serverId, index) => ({ serverId, index }))

  const compare = (
    a: { serverId: number; index: number },
    b: { serverId: number; index: number }
  ): number => {
    const left = servers[a.serverId]
    const right = servers[b.serverId]

    switch (sort) {
      case "attention":
        return attentionScore(right) - attentionScore(left)
      case "name":
        return left.serverName.localeCompare(right.serverName, undefined, {
          sensitivity: "base",
          numeric: true,
        })
      case "cpu":
        return (right.cpu?.percent ?? -1) - (left.cpu?.percent ?? -1)
      case "memory":
        return memoryPercent(right) - memoryPercent(left)
    }
  }

  return indexed
    .sort((a, b) => compare(a, b) || a.index - b.index)
    .map((entry) => entry.serverId)
}
