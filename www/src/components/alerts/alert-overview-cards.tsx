import { MetricStatCard } from "@/components/metrics/metric-stat-card"
import type { AlertOverview } from "@/lib/alerts/overview"
import { formatCount } from "@/lib/formatter"
import { attentionCountColorClass } from "@/lib/metrics/percent-level"

function plural(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? "" : "s"}`
}

function AlertOverviewCards({
  overview,
  serverCount,
}: {
  overview: AlertOverview
  serverCount: number
}) {
  const formatInteger = (value: number) => formatCount(Math.round(value))

  return (
    <div className="metric-stat-grid">
      <MetricStatCard
        title="Firing now"
        value={overview.firing}
        formatValue={formatInteger}
        detail={
          overview.firing === 0 ? "Everything is healthy" : "Needs attention"
        }
        valueClassName={attentionCountColorClass(overview.firing)}
        animate={false}
      />
      <MetricStatCard
        title="Alerts"
        value={overview.ruleCount}
        formatValue={formatInteger}
        detail={"In enabled groups"}
        animate={false}
      />
      <MetricStatCard
        title="Servers covered"
        value={overview.coveredServerCount}
        formatValue={formatInteger}
        valueSuffix={`of ${serverCount}`}
        detail={
          overview.uncoveredServers.length === 0
            ? "Every server is monitored"
            : `${plural(overview.uncoveredServers.length, "server")} in no enabled group`
        }
        valueClassName={
          overview.uncoveredServers.length > 0 ? "text-warning" : undefined
        }
        animate={false}
      />
    </div>
  )
}

export { AlertOverviewCards }
