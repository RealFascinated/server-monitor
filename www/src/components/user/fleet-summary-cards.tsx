import { memo } from "react"

import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import type { ServerResponse } from "@/lib/api/user/servers"
import { formatCount, formatPercentValue } from "@/lib/formatter"
import {
  percentLevelColorClass,
  fleetOnlineStatusColorClass,
  attentionCountColorClass,
} from "@/lib/metrics/percent-level"
import type { FleetFilter } from "@/lib/servers/fleet-filter"
import { computeFleetSummary } from "@/lib/servers/fleet-summary"

type FleetSummaryCardsProps = {
  servers: ServerResponse[]
  activeFilter: FleetFilter
  onFilterChange: (filter: FleetFilter) => void
}

function FleetSummaryCardsInner({
  servers,
  activeFilter,
  onFilterChange,
}: FleetSummaryCardsProps) {
  const summary = computeFleetSummary(servers)

  function onlineMetricDetail(
    avg: number | null,
    count: number,
    noDataLabel: string
  ): string {
    if (summary.online === 0) {
      return "No online servers"
    }

    if (avg == null) {
      return noDataLabel
    }

    return `Across ${count} online server${count === 1 ? "" : "s"}`
  }

  const attentionCount = summary.needsAttentionCount
  const attentionDetail =
    attentionCount === 0
      ? "No issues detected"
      : "Offline or high CPU, memory, or disk"

  return (
    <div className="metric-stat-grid">
      <MetricStatCard
        title="Fleet status"
        value={summary.online}
        formatValue={(value) => formatCount(Math.round(value))}
        valueSuffix="online"
        detail={`${summary.offline} offline · ${summary.pending} pending`}
        animate={false}
        valueClassName={fleetOnlineStatusColorClass(
          summary.online,
          summary.offline
        )}
      />
      <MetricStatCard
        title="Avg CPU"
        value={summary.avgCpuPercent ?? 0}
        formatValue={(value) =>
          summary.avgCpuPercent == null ? "-" : formatPercentValue(value)
        }
        detail={onlineMetricDetail(
          summary.avgCpuPercent,
          summary.onlineWithCpuCount,
          "No CPU data yet"
        )}
        valueClassName={percentLevelColorClass(summary.avgCpuPercent)}
        progress={summary.avgCpuPercent}
        animate={false}
      />
      <MetricStatCard
        title="Avg memory"
        value={summary.avgMemPercent ?? 0}
        formatValue={(value) =>
          summary.avgMemPercent == null ? "-" : formatPercentValue(value)
        }
        detail={onlineMetricDetail(
          summary.avgMemPercent,
          summary.onlineWithMemCount,
          "No memory data yet"
        )}
        valueClassName={percentLevelColorClass(summary.avgMemPercent)}
        progress={summary.avgMemPercent}
        animate={false}
      />
      <MetricStatCard
        title="Needs attention"
        value={attentionCount}
        formatValue={(value) => formatCount(Math.round(value))}
        detail={attentionDetail}
        valueClassName={attentionCountColorClass(attentionCount)}
        animate={false}
        onSelect={() =>
          onFilterChange(activeFilter === "attention" ? "all" : "attention")
        }
        selected={activeFilter === "attention"}
        selectLabel={
          activeFilter === "attention"
            ? "Needs attention: showing only these servers. Click to show all servers."
            : `Needs attention: ${attentionCount}. Click to show only these servers.`
        }
      />
    </div>
  )
}

const FleetSummaryCards = memo(FleetSummaryCardsInner)

export { FleetSummaryCards }
