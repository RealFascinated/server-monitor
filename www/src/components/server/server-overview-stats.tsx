import { useQuery } from "@tanstack/react-query"

import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import { useUserServer } from "@/hooks/use-user-server"
import { userServerSparklineQueryOptions } from "@/lib/api/user/sparklines.queries"
import type { ServerResponse } from "@/lib/api/user/servers"
import {
  formatCpuInventoryTooltip,
  formatMemoryBytes,
  formatPercentValue,
  memoryUsagePercent,
} from "@/lib/formatter"
import { percentLevelColorClass } from "@/lib/metrics/percent-level"

function serverOverviewHasData(
  server: Pick<ServerResponse, "cpu" | "memory" | "disk"> | undefined
): boolean {
  return (
    server?.cpu?.percent != null ||
    server?.memory?.usage != null ||
    server?.disk?.usage != null
  )
}

type ServerOverviewStatsProps = {
  serverId: number
}

function ServerOverviewStats({ serverId }: ServerOverviewStatsProps) {
  const { data: server } = useUserServer(serverId)
  const { data: sparklines } = useQuery({
    ...userServerSparklineQueryOptions(serverId),
    enabled: server != null,
  })

  if (!server) {
    return null
  }

  const cpuUsage = server.cpu?.percent ?? null
  const cpuModel = server.inventory?.cpuModel ?? null
  const cpuInventoryTooltip = formatCpuInventoryTooltip(server.inventory)
  const memUsage = server.memory?.usage ?? null
  const memTotal = server.memory?.max ?? null
  const memPercent = memoryUsagePercent(memUsage, memTotal)

  const diskUsage = server.disk?.usage ?? null
  const diskTotal = server.disk?.max ?? null
  const diskPercent = memoryUsagePercent(diskUsage, diskTotal)

  const stats = [
    cpuUsage != null ? (
      <MetricStatCard
        key="cpu"
        title="CPU"
        value={cpuUsage}
        formatValue={formatPercentValue}
        detail={cpuModel ?? undefined}
        detailTooltip={cpuInventoryTooltip ?? undefined}
        valueClassName={percentLevelColorClass(cpuUsage)}
        progress={cpuUsage}
        trend={sparklines?.cpu}
        trendLabel="CPU, last hour"
      />
    ) : null,
    memUsage != null ? (
      <MetricStatCard
        key="memory"
        title="Memory"
        value={memPercent ?? 0}
        formatValue={(value) =>
          memPercent == null ? "-" : formatPercentValue(value)
        }
        detail={
          memTotal != null
            ? `${formatMemoryBytes(memUsage)} of ${formatMemoryBytes(memTotal)}`
            : formatMemoryBytes(memUsage)
        }
        valueClassName={percentLevelColorClass(memPercent)}
        progress={memPercent}
        trend={sparklines?.memory}
        trendLabel="Memory, last hour"
      />
    ) : null,
    diskUsage != null ? (
      <MetricStatCard
        key="disk"
        title="Root Disk"
        value={diskPercent ?? 0}
        formatValue={(value) =>
          diskPercent == null ? "-" : formatPercentValue(value)
        }
        detail={
          diskTotal != null
            ? `${formatMemoryBytes(diskUsage)} of ${formatMemoryBytes(diskTotal)}`
            : formatMemoryBytes(diskUsage)
        }
        valueClassName={percentLevelColorClass(diskPercent)}
        progress={diskPercent}
      />
    ) : null,
  ].filter(Boolean)

  if (stats.length === 0) {
    return null
  }

  return <div className="metric-stat-grid">{stats}</div>
}

export { ServerOverviewStats, serverOverviewHasData }
