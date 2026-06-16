import type {
  FleetMetrics,
  FleetOsMetrics,
  FleetVersionMetrics,
  HttpMetrics,
  HttpMetricsEntry,
  IngestMetrics,
  JvmMetrics,
  OverviewMetrics,
  VmMetrics,
} from "@/lib/api/admin/metrics"
import {
  formatCount,
  formatDurationSeconds,
  formatMemoryBytes,
  formatMilliseconds,
  formatPerMinute,
  formatPercentValue,
} from "@/lib/formatter"
import type { MetricChartConfig } from "@/lib/metrics/chart-config"
import { chartSeries, getLatestValue, hasValues } from "@/lib/metrics/series"

function parseHttpEntries(
  http: HttpMetrics | null | undefined
): HttpMetricsEntry[] {
  if (!http) {
    return []
  }

  return Object.values(http)
    .filter((entry) => hasValues(entry.httpRequestsTotal))
    .sort((left, right) => {
      const leftLatest = getLatestValue(left.httpRequestsTotal) ?? 0
      const rightLatest = getLatestValue(right.httpRequestsTotal) ?? 0
      return rightLatest - leftLatest
    })
}

function overviewCharts(overview: OverviewMetrics): MetricChartConfig[] {
  return [
    {
      title: "Users",
      description:
        "Total registered users and users created in the last 24 hours.",
      series: [
        chartSeries("Users", overview.users),
        chartSeries("New (24h)", overview.usersNew24h),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "Active sessions",
      description: "Signed-in user sessions that have not expired.",
      series: [chartSeries("Sessions", overview.activeSessions)],
      valueFormatter: formatCount,
    },
    {
      title: "Database size",
      description: "Total database storage used by Monitor.",
      series: [chartSeries("Size", overview.databaseSizeBytes)],
      valueFormatter: formatMemoryBytes,
    },
  ]
}

function fleetCharts(fleet: FleetMetrics): MetricChartConfig[] {
  return [
    {
      title: "Server status",
      description:
        "Current online, offline, pending, and total registered servers.",
      series: [
        chartSeries("Online", fleet.serversOnline),
        chartSeries("Offline", fleet.serversOffline),
        chartSeries("Pending", fleet.serversPending),
        chartSeries("Total", fleet.serversTotal),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "New servers (24h)",
      description: "Servers registered in the last 24 hours.",
      series: [chartSeries("New (24h)", fleet.serversNew24h)],
      valueFormatter: formatCount,
    },
  ]
}

function fleetOsCharts(entries: FleetOsMetrics[]): MetricChartConfig[] {
  const sorted = [...entries].sort((left, right) =>
    left.os.localeCompare(right.os)
  )

  return [
    {
      title: "Servers by OS",
      description: "Registered servers grouped by operating system.",
      series: sorted.map((entry) => chartSeries(entry.os, entry.serversByOs)),
      valueFormatter: formatCount,
      showCurrentValues: false,
    },
  ]
}

function fleetVersionCharts(
  entries: FleetVersionMetrics[]
): MetricChartConfig[] {
  const sorted = [...entries].sort((left, right) =>
    left.version.localeCompare(right.version)
  )

  return [
    {
      title: "Servers by agent version",
      description: "Registered servers grouped by Monitor Agent version.",
      series: sorted.map((entry) =>
        chartSeries(entry.version, entry.serversByAgentVersion)
      ),
      valueFormatter: formatCount,
      showCurrentValues: false,
    },
  ]
}

function ingestCharts(ingest: IngestMetrics): MetricChartConfig[] {
  return [
    {
      title: "Ingests per minute",
      description:
        "Metric ingest requests received per minute (Prometheus rate).",
      series: [chartSeries("Ingests", ingest.ingestsTotal)],
      valueFormatter: formatPerMinute,
    },
    {
      title: "Auth failures per minute",
      description:
        "Ingest requests rejected due to invalid authentication, per minute.",
      series: [chartSeries("Failures", ingest.ingestAuthFailuresTotal)],
      valueFormatter: formatPerMinute,
    },
    {
      title: "Ingest duration",
      description: "Mean time to process an ingest request.",
      series: [chartSeries("Duration", ingest.ingestDurationSeconds)],
      valueFormatter: (value) => formatMilliseconds(value * 1000),
    },
    {
      title: "Ingest payload size",
      description: "Mean ingest payload size.",
      series: [chartSeries("Payload", ingest.ingestPayloadBytes)],
      valueFormatter: formatMemoryBytes,
    },
  ]
}

function jvmCharts(jvm: JvmMetrics): MetricChartConfig[] {
  const charts: MetricChartConfig[] = [
    {
      title: "Heap memory",
      description: "JVM heap used, max, and non-heap memory.",
      series: [
        chartSeries("Used", jvm.jvmHeapUsedBytes),
        chartSeries("Max", jvm.jvmHeapMaxBytes),
        chartSeries("Non-heap", jvm.jvmNonheapUsedBytes),
      ],
      valueFormatter: formatMemoryBytes,
    },
    {
      title: "CPU load",
      description: "JVM process CPU utilization (0–100%).",
      series: [chartSeries("CPU", jvm.jvmProcessCpuLoad)],
      valueFormatter: (value) => formatPercentValue(value * 100),
    },
    {
      title: "Threads",
      description: "Live JVM thread count.",
      series: [chartSeries("Threads", jvm.jvmThreadCount)],
      valueFormatter: formatCount,
    },
    {
      title: "Uptime",
      description: "Time since the Monitor backend JVM started.",
      series: [chartSeries("Uptime", jvm.jvmUptimeSeconds)],
      valueFormatter: formatDurationSeconds,
    },
  ]

  if (hasValues(jvm.jvmProcessRssBytes)) {
    charts.splice(1, 0, {
      title: "Process RSS",
      description: "Resident set size of the Monitor backend process.",
      series: [chartSeries("RSS", jvm.jvmProcessRssBytes)],
      valueFormatter: formatMemoryBytes,
    })
  }

  return charts
}

function vmCharts(vm: VmMetrics): MetricChartConfig[] {
  return [
    {
      title: "Storage size",
      description: "Total VictoriaMetrics time-series data stored on disk.",
      series: [chartSeries("Size", vm.vmStorageSizeBytes)],
      valueFormatter: formatMemoryBytes,
      yRange: { autoMin: true },
    },
    {
      title: "Datapoint count",
      description: "Total samples stored in VictoriaMetrics.",
      series: [chartSeries("Datapoints", vm.vmDatapointCount)],
      valueFormatter: formatCount,
      yRange: { autoMin: true },
    },
    {
      title: "Queries and writes per minute",
      description: "VictoriaMetrics query and write operations per minute.",
      series: [
        chartSeries("Queries", vm.vmQueriesTotal),
        chartSeries("Writes", vm.vmWritesTotal),
      ],
      valueFormatter: formatPerMinute,
    },
    {
      title: "Errors per minute",
      description: "VictoriaMetrics query and write errors per minute.",
      series: [
        chartSeries("Query errors", vm.vmQueryErrorsTotal),
        chartSeries("Write errors", vm.vmWriteErrorsTotal),
      ],
      valueFormatter: formatPerMinute,
    },
    {
      title: "Query duration",
      description: "Mean VictoriaMetrics query latency.",
      series: [chartSeries("Duration", vm.vmQueryDurationSeconds)],
      valueFormatter: (value) => formatMilliseconds(value * 1000),
    },
    {
      title: "Write duration",
      description: "Mean VictoriaMetrics write latency.",
      series: [chartSeries("Duration", vm.vmWriteDurationSeconds)],
      valueFormatter: (value) => formatMilliseconds(value * 1000),
    },
  ]
}

function httpCharts(entries: HttpMetricsEntry[]): MetricChartConfig[] {
  return [
    {
      title: "Requests per minute",
      description:
        "HTTP request rate by method, path, and status (Prometheus rate).",
      series: entries.map((entry) =>
        chartSeries(
          `${entry.method} ${entry.path} (${entry.status})`,
          entry.httpRequestsTotal
        )
      ),
      valueFormatter: formatPerMinute,
      showCurrentValues: false,
    },
  ]
}

export type { MetricChartConfig } from "@/lib/metrics/chart-config"
export {
  fleetCharts,
  fleetOsCharts,
  fleetVersionCharts,
  httpCharts,
  ingestCharts,
  jvmCharts,
  overviewCharts,
  parseHttpEntries,
  vmCharts,
}
