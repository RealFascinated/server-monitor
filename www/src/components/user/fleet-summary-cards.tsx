import { memo } from "react"

import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import type { ServerResponse } from "@/lib/api/user/servers"
import { formatCount, formatPercentValue } from "@/lib/formatter"
import { percentLevelColorClass } from "@/lib/metrics/percent-level"
import { computeFleetSummary } from "@/lib/servers/fleet-summary"
import { cn } from "@/lib/utils"

function FleetSummaryCardsInner({ servers }: { servers: ServerResponse[] }) {
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
        formatValue={(value) => `${formatCount(Math.round(value))} online`}
        detail={`${summary.offline} offline · ${summary.pending} pending`}
        animate={false}
        valueClassName={cn(
          summary.online > 0 && summary.offline === 0
            ? "text-[#2E9470] dark:text-green-400"
            : summary.online === 0
              ? "text-[#C44E4E] dark:text-error"
              : undefined
        )}
      />
      <MetricStatCard
        title="Avg CPU"
        value={summary.avgCpuPercent ?? 0}
        formatValue={(value) =>
          summary.avgCpuPercent == null ? "—" : formatPercentValue(value)
        }
        detail={onlineMetricDetail(
          summary.avgCpuPercent,
          summary.onlineWithCpuCount,
          "No CPU data yet"
        )}
        valueClassName={percentLevelColorClass(summary.avgCpuPercent)}
        animate={false}
      />
      <MetricStatCard
        title="Avg memory"
        value={summary.avgMemPercent ?? 0}
        formatValue={(value) =>
          summary.avgMemPercent == null ? "—" : formatPercentValue(value)
        }
        detail={onlineMetricDetail(
          summary.avgMemPercent,
          summary.onlineWithMemCount,
          "No memory data yet"
        )}
        valueClassName={percentLevelColorClass(summary.avgMemPercent)}
        animate={false}
      />
      <MetricStatCard
        title="Needs attention"
        value={attentionCount}
        formatValue={(value) => formatCount(Math.round(value))}
        detail={attentionDetail}
        valueClassName={cn(
          attentionCount === 0
            ? "text-[#2E9470] dark:text-green-400"
            : "text-[#B8870A] dark:text-warning"
        )}
        animate={false}
      />
    </div>
  )
}

const FleetSummaryCards = memo(FleetSummaryCardsInner)

export { FleetSummaryCards }
