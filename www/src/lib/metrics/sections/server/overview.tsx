import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import { useUserServer } from "@/hooks/use-user-server"
import type { ServerResponse } from "@/lib/api/user/servers"
import {
  formatCpuInventoryTooltip,
  formatMemoryBytes,
  formatPercentValue,
  memoryUsagePercent,
} from "@/lib/formatter"
import { percentLevelColorClass } from "@/lib/metrics/percent-level"

function overviewHasData(
  server:
    | Pick<ServerResponse, "cpu" | "memory" | "disk">
    | undefined
): boolean {
  return (
    server?.cpu?.percent != null ||
    server?.memory?.usage != null ||
    server?.disk?.usage != null
  )
}

function OverviewStats({ serverId }: { serverId: number }) {
  const { data: server } = useUserServer(serverId)
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
      />
    ) : null,
    memUsage != null ? (
      <MetricStatCard
        key="memory"
        title="Memory"
        value={memPercent ?? 0}
        formatValue={(value) =>
          memPercent == null ? "—" : formatPercentValue(value)
        }
        detail={
          memTotal != null
            ? `${formatMemoryBytes(memUsage)} of ${formatMemoryBytes(memTotal)}`
            : formatMemoryBytes(memUsage)
        }
        valueClassName={percentLevelColorClass(memPercent)}
      />
    ) : null,
    diskUsage != null ? (
      <MetricStatCard
        key="disk"
        title="Root Disk"
        value={diskPercent ?? 0}
        formatValue={(value) =>
          diskPercent == null ? "—" : formatPercentValue(value)
        }
        detail={
          diskTotal != null
            ? `${formatMemoryBytes(diskUsage)} of ${formatMemoryBytes(diskTotal)}`
            : formatMemoryBytes(diskUsage)
        }
        valueClassName={percentLevelColorClass(diskPercent)}
      />
    ) : null,
  ].filter(Boolean)

  if (stats.length === 0) {
    return null
  }

  return <div className="metric-stat-grid">{stats}</div>
}

export { OverviewStats, overviewHasData }
