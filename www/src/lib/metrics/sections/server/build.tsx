import {
  Cable,
  Container,
  Cpu,
  Database,
  Disc,
  Gpu,
  HardDrive,
  LayoutDashboard,
  MemoryStick,
  Network,
  Thermometer,
  Cog,
} from "lucide-react"

import type { ServerMetricsResponse } from "@/lib/api/user/metrics"
import type { ServerResponse } from "@/lib/api/user/servers"
import { TEMPERATURE_THRESHOLDS } from "@/lib/metrics/chart-thresholds"
import { createMetricsSectionBuilder } from "@/lib/metrics/sections/builder"
import { addChartSection } from "@/lib/metrics/sections/chart-section"
import { metricSectionId } from "@/lib/metrics/sections/id"
import { getLatestValue, chartSeries } from "@/lib/metrics/series"
import type { MetricsSectionNode } from "@/lib/metrics/sections/types"
import {
  containerCharts,
  diskCharts,
  gpuCharts,
  hostCpuCharts,
  hostMemoryCharts,
  hostProcessCharts,
  networkCharts,
  zfsPoolCharts,
} from "@/lib/metrics/sections/server/charts"
import {
  OverviewStats,
  overviewHasData,
} from "@/lib/metrics/sections/server/overview"
import type { MetricsTimeGrid } from "@/lib/metrics/timestamps"
import {
  formatCelsius,
  formatCount,
  formatMemoryBytes,
  formatNumber,
} from "@/lib/formatter"

type BuildServerMetricSectionsOptions = {
  includeOverview?: boolean
}

function buildServerMetricSections(
  metrics: ServerMetricsResponse,
  timeGrid: MetricsTimeGrid,
  server?: ServerResponse,
  options: BuildServerMetricSectionsOptions = {}
): MetricsSectionNode[] {
  const { includeOverview = true } = options
  const host = metrics.host ?? {}
  const builder = createMetricsSectionBuilder()

  if (includeOverview && overviewHasData(server)) {
    builder.leaf({
      title: "Overview",
      icon: LayoutDashboard,
      render: () => <OverviewStats serverId={metrics.id} />,
    })
  }

  addChartSection(builder, {
    title: "CPU",
    icon: Cpu,
    charts: hostCpuCharts(host, metrics.cpuCores),
    timeGrid,
  })

  addChartSection(builder, {
    title: "Memory",
    icon: MemoryStick,
    charts: hostMemoryCharts(host),
    timeGrid,
  })

  addChartSection(builder, {
    title: "Processes",
    icon: Cog,
    charts: hostProcessCharts(host),
    timeGrid,
  })

  builder.group({ id: "disks", title: "Disk", icon: HardDrive }, (group) => {
    for (const disk of metrics.disks ?? []) {
      const usedBytes = getLatestValue(disk.usedBytes)
      const totalBytes = getLatestValue(disk.totalBytes)
      const navPercentTooltip =
        usedBytes != null && totalBytes != null
          ? `${formatMemoryBytes(usedBytes)} of ${formatMemoryBytes(totalBytes)}`
          : usedBytes != null
            ? formatMemoryBytes(usedBytes)
            : undefined

      addChartSection(group, {
        id: metricSectionId(`disk-${disk.disk}`),
        title: `Disk ${disk.disk}`,
        navLabel: disk.disk,
        navPercent: getLatestValue(disk.usagePercent),
        navPercentTooltip,
        icon: HardDrive,
        charts: diskCharts(disk),
        timeGrid,
      })
    }
  })

  builder.group(
    { id: "networks", title: "Network", icon: Network },
    (group) => {
      for (const network of metrics.networks ?? []) {
        addChartSection(group, {
          id: metricSectionId(`network-${network.interface}`),
          title: `Network ${network.interface}`,
          navLabel: network.interface,
          icon: Network,
          charts: networkCharts(network),
          timeGrid,
        })
      }
    }
  )

  if ((metrics.tcpConnections ?? []).length > 0) {
    addChartSection(builder, {
      title: "TCP",
      icon: Cable,
      charts: [
        {
          title: "Connections by state",
          description:
            "TCP connections grouped by state (for example ESTABLISHED, TIME_WAIT).",
          series: (metrics.tcpConnections ?? []).map((tcp) =>
            chartSeries(tcp.state, tcp.connections)
          ),
          valueFormatter: formatCount,
          showCurrentValues: false,
        },
      ],
      timeGrid,
    })
  }

  builder.group({ id: "gpus", title: "GPU", icon: Gpu }, (group) => {
    for (const gpu of metrics.gpus ?? []) {
      addChartSection(group, {
        id: metricSectionId(`${gpu.gpu}-${gpu.deviceId}`),
        title: gpu.gpu,
        navLabel: gpu.gpu,
        icon: Gpu,
        description: `${gpu.vendor} · ${gpu.deviceId}`,
        charts: gpuCharts(gpu),
        timeGrid,
      })
    }
  })

  addChartSection(builder, {
    title: "Docker",
    icon: Container,
    charts: containerCharts(metrics.containers),
    timeGrid,
  })

  if ((metrics.temperatures ?? []).length > 0) {
    addChartSection(builder, {
      title: "Temperature",
      icon: Thermometer,
      charts: [
        {
          title: "Sensors",
          description: "Hardware temperature readings from system sensors.",
          series: (metrics.temperatures ?? []).map((sensor) =>
            chartSeries(sensor.sensor, sensor.temperatureCelsius)
          ),
          valueFormatter: formatCelsius,
          thresholds: TEMPERATURE_THRESHOLDS,
          showCurrentValues: false,
        },
      ],
      timeGrid,
    })
  }

  builder.group(
    { id: "zfs", title: "ZFS", icon: Disc, showChildCount: false },
    (zfs) => {
      if (metrics.zfsArc) {
        addChartSection(zfs, {
          title: "ARC",
          icon: Disc,
          charts: [
            {
              title: "ARC size",
              description:
                "ZFS adaptive replacement cache size vs target, max, and min limits.",
              series: [
                chartSeries("Size", metrics.zfsArc.sizeBytes),
                chartSeries("Target", metrics.zfsArc.targetBytes),
                chartSeries("Max", metrics.zfsArc.maxBytes),
                chartSeries("Min", metrics.zfsArc.minBytes),
              ],
              valueFormatter: formatMemoryBytes,
            },
            {
              title: "ARC composition",
              description: "Data, metadata, and L2ARC cache breakdown.",
              series: [
                chartSeries("Data", metrics.zfsArc.dataBytes),
                chartSeries("Metadata", metrics.zfsArc.metadataBytes),
                chartSeries("L2ARC", metrics.zfsArc.l2arcSizeBytes),
              ],
              valueFormatter: formatMemoryBytes,
            },
            {
              title: "ARC efficiency",
              description: "Cache hit ratio and misses per second.",
              series: [
                chartSeries("Hit ratio", metrics.zfsArc.hitRatio),
                chartSeries("Misses/s", metrics.zfsArc.missesPerSecond),
              ],
              valueFormatter: formatNumber,
            },
          ],
          timeGrid,
        })
      }

      zfs.group(
        { id: "zfs-pools", title: "Pools", icon: Database },
        (group) => {
          for (const pool of metrics.zfsPools ?? []) {
            addChartSection(group, {
              id: metricSectionId(`zfs-pool-${pool.pool}`),
              title: `ZFS pool ${pool.pool}`,
              navLabel: pool.pool,
              icon: Database,
              description: `Health: ${pool.health} · Scan: ${pool.scanState}`,
              charts: zfsPoolCharts(pool),
              timeGrid,
            })
          }
        }
      )
    }
  )

  return builder.build()
}

export { buildServerMetricSections }
export type { BuildServerMetricSectionsOptions }
