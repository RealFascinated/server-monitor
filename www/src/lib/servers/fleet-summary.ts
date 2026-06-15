import type { ServerResponse } from "@/lib/api/user/servers"
import { memoryUsagePercent } from "@/lib/formatter"
import { getPercentLevel } from "@/lib/metrics/percent-level"

function isHighUsage(value: number | null): boolean {
  if (value == null) {
    return false
  }

  const level = getPercentLevel(value)
  return level === "warning" || level === "critical"
}

export function serverNeedsAttention(server: ServerResponse): boolean {
  if (server.status === "OFFLINE") {
    return true
  }

  if (server.status !== "ONLINE") {
    return false
  }

  const memPercent = memoryUsagePercent(server.memUsage, server.memMax)
  const diskPercent = memoryUsagePercent(server.diskUsage, server.diskMax)

  return (
    isHighUsage(server.cpuPercent) ||
    isHighUsage(memPercent) ||
    isHighUsage(diskPercent)
  )
}

export type FleetSummary = {
  online: number
  offline: number
  pending: number
  avgCpuPercent: number | null
  onlineWithCpuCount: number
  avgMemPercent: number | null
  onlineWithMemCount: number
  needsAttentionCount: number
}

export function computeFleetSummary(servers: ServerResponse[]): FleetSummary {
  let online = 0
  let offline = 0
  let pending = 0
  let cpuSum = 0
  let cpuCount = 0
  let memSum = 0
  let memCount = 0
  let needsAttentionCount = 0

  for (const server of servers) {
    switch (server.status) {
      case "ONLINE":
        online++
        if (server.cpuPercent != null) {
          cpuSum += server.cpuPercent
          cpuCount++
        }
        {
          const memPercent = memoryUsagePercent(server.memUsage, server.memMax)
          if (memPercent != null) {
            memSum += memPercent
            memCount++
          }
        }
        break
      case "OFFLINE":
        offline++
        break
      case "PENDING":
        pending++
        break
    }

    if (serverNeedsAttention(server)) {
      needsAttentionCount++
    }
  }

  return {
    online,
    offline,
    pending,
    avgCpuPercent: cpuCount > 0 ? cpuSum / cpuCount : null,
    onlineWithCpuCount: cpuCount,
    avgMemPercent: memCount > 0 ? memSum / memCount : null,
    onlineWithMemCount: memCount,
    needsAttentionCount,
  }
}
