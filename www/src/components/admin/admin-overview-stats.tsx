import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import type { AdminMetricsResponse } from "@/lib/api/admin/metrics"
import { formatCount, formatMemoryBytes } from "@/lib/formatter"
import { getLatestValue, hasValues } from "@/lib/metrics/series"
import { cn } from "@/lib/utils"

function adminOverviewHasData(metrics: AdminMetricsResponse): boolean {
  const overview = metrics.overview ?? {}
  const fleet = metrics.fleet ?? {}

  return (
    hasValues(overview.users) ||
    hasValues(overview.activeSessions) ||
    hasValues(overview.databaseSizeBytes) ||
    hasValues(fleet.serversOnline)
  )
}

function AdminOverviewStats({ metrics }: { metrics: AdminMetricsResponse }) {
  const overview = metrics.overview ?? {}
  const fleet = metrics.fleet ?? {}

  const users = getLatestValue(overview.users)
  const usersNew24h = getLatestValue(overview.usersNew24h)
  const activeSessions = getLatestValue(overview.activeSessions)
  const databaseSizeBytes = getLatestValue(overview.databaseSizeBytes)
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
    activeSessions != null ? (
      <MetricStatCard
        key="sessions"
        title="Active sessions"
        value={activeSessions}
        formatValue={(value) => formatCount(Math.round(value))}
        detail="Signed-in sessions that have not expired"
      />
    ) : null,
    serversOnline != null ? (
      <MetricStatCard
        key="servers"
        title="Servers online"
        value={serversOnline}
        formatValue={(value) => `${formatCount(Math.round(value))} online`}
        detail={fleetDetail}
        valueClassName={cn(
          serversOnline > 0 && (serversOffline ?? 0) === 0
            ? "text-[#2E9470] dark:text-green-400"
            : serversOnline === 0
              ? "text-[#C44E4E] dark:text-error"
              : undefined
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
  ].filter(Boolean)

  if (stats.length === 0) {
    return null
  }

  return <div className="metric-stat-grid">{stats}</div>
}

export { AdminOverviewStats, adminOverviewHasData }
