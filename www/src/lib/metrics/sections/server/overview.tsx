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
    | Pick<ServerResponse, "cpuPercent" | "memUsage" | "diskUsage">
    | undefined
): boolean {
  return (
    server?.cpuPercent != null ||
    server?.memUsage != null ||
    server?.diskUsage != null
  )
}

function OverviewStats({ serverId }: { serverId: number }) {
  const { data: server } = useUserServer(serverId)
  if (!server) {
    return null
  }

  const cpuUsage = server.cpuPercent ?? null
  const cpuModel = server.inventory?.cpuModel ?? null
  const cpuInventoryTooltip = formatCpuInventoryTooltip(server.inventory)
  const memUsage = server.memUsage ?? null
  const memTotal = server.memMax ?? null
  const memPercent = memoryUsagePercent(memUsage, memTotal)

  const diskUsage = server.diskUsage ?? null
  const diskTotal = server.diskMax ?? null
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
