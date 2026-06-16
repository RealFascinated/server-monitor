import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import type { AdminMetricsResponse } from "@/lib/api/admin/metrics"
import { formatCompactCount, formatCount, formatMemoryBytes } from "@/lib/formatter"
import { getLatestValue, hasValues } from "@/lib/metrics/series"
import { fleetOnlineStatusColorClass } from "@/lib/metrics/percent-level"

function adminOverviewHasData(metrics: AdminMetricsResponse): boolean {
  const overview = metrics.overview ?? {}
  const fleet = metrics.fleet ?? {}
  const vm = metrics.vm ?? {}

  return (
    hasValues(overview.users) ||
    hasValues(overview.databaseSizeBytes) ||
    hasValues(vm.vmDatapointCount) ||
    hasValues(vm.vmStorageSizeBytes) ||
    hasValues(fleet.serversOnline)
  )
}

function AdminOverviewStats({ metrics }: { metrics: AdminMetricsResponse }) {
  const overview = metrics.overview ?? {}
  const fleet = metrics.fleet ?? {}
  const vm = metrics.vm ?? {}

  const users = getLatestValue(overview.users)
  const usersNew24h = getLatestValue(overview.usersNew24h)
  const databaseSizeBytes = getLatestValue(overview.databaseSizeBytes)
  const vmDatapointCount = getLatestValue(vm.vmDatapointCount)
  const vmStorageSizeBytes = getLatestValue(vm.vmStorageSizeBytes)
  const serversOnline = getLatestValue(fleet.serversOnline)
  const serversOffline = getLatestValue(fleet.serversOffline)
  const serversPending = getLatestValue(fleet.serversPending)

  const usersNewDetail =
    usersNew24h == null
      ? undefined
      : usersNew24h === 0
        ? "No new users in 24h"
        : `${formatCount(Math.round(usersNew24h))} new in 24h`

  const fleetDetail =
    serversOffline == null && serversPending == null
      ? undefined
      : `${formatCount(Math.round(serversOffline ?? 0))} offline · ${formatCount(Math.round(serversPending ?? 0))} pending`

  const stats = [
    users != null ? (
      <MetricStatCard
        key="users"
        title="Users"
        value={users}
        formatValue={(value) => formatCount(Math.round(value))}
        detail={usersNewDetail}
      />
    ) : null,
    serversOnline != null ? (
      <MetricStatCard
        key="servers"
        title="Servers online"
        value={serversOnline}
        formatValue={(value) => `${formatCount(Math.round(value))} online`}
        detail={fleetDetail}
        valueClassName={fleetOnlineStatusColorClass(
          serversOnline,
          serversOffline ?? 0
        )}
      />
    ) : null,
    databaseSizeBytes != null ? (
      <MetricStatCard
        key="database"
        title="Database size"
        value={databaseSizeBytes}
        formatValue={formatMemoryBytes}
        detail="Total storage used by Monitor"
      />
    ) : null,
    vmStorageSizeBytes != null ? (
      <MetricStatCard
        key="vm-storage"
        title="VictoriaMetrics size"
        value={vmStorageSizeBytes}
        formatValue={formatMemoryBytes}
        detail={
          vmDatapointCount == null
            ? "Total time-series data stored on disk"
            : `${formatCompactCount(Math.round(vmDatapointCount))} datapoints stored`
        }
      />
    ) : null,
    vmDatapointCount != null && vmStorageSizeBytes == null ? (
      <MetricStatCard
        key="vm-datapoints"
        title="VictoriaMetrics datapoints"
        value={vmDatapointCount}
        formatValue={(value) => formatCompactCount(Math.round(value))}
        detail="Total samples stored in VictoriaMetrics"
      />
    ) : null,
  ].filter(Boolean)

  if (stats.length === 0) {
    return null
  }

  return <div className="metric-stat-grid">{stats}</div>
}

export { AdminOverviewStats, adminOverviewHasData }
