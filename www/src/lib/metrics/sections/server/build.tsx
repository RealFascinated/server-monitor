import {
  Cable,
  Container,
  Cpu,
  Database,
  Disc,
  Gauge,
  Gpu,
  HardDrive,
  MemoryStick,
  Network,
  Thermometer,
} from "lucide-react"

import type { ServerMetricsResponse } from "@/lib/api/user/metrics"
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
  hostHardwareCharts,
  hostMemoryCharts,
  hostSystemCharts,
  networkCharts,
  tcpCharts,
  zfsPoolCharts,
} from "@/lib/metrics/sections/server/charts"
import type { MetricsTimeGrid } from "@/lib/metrics/timestamps"
import { formatMemoryBytes, formatNumber } from "@/lib/formatter"

function buildServerMetricSections(
  metrics: ServerMetricsResponse,
  timeGrid: MetricsTimeGrid
): MetricsSectionNode[] {
  const host = metrics.host ?? {}
  const builder = createMetricsSectionBuilder()

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
    title: "System",
    icon: Gauge,
    charts: hostSystemCharts(host),
    timeGrid,
  })

  addChartSection(builder, {
    title: "Hardware",
    icon: Thermometer,
    charts: hostHardwareCharts(host, metrics.temperatures),
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
        sectionKind: "Disk",
        title: disk.disk,
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
      group.group(
        { id: "network-interfaces", title: "Interfaces", icon: Network },
        (interfaces) => {
          for (const network of metrics.networks ?? []) {
            addChartSection(interfaces, {
              id: metricSectionId(`network-${network.interface}`),
              sectionKind: "Interface",
              title: network.interface,
              navLabel: network.interface,
              icon: Network,
              charts: networkCharts(network),
              timeGrid,
            })
          }
        }
      )

      addChartSection(group, {
        title: "TCP",
        icon: Cable,
        charts: tcpCharts(metrics.tcpConnections),
        timeGrid,
      })
    }
  )

  builder.group({ id: "gpus", title: "GPU", icon: Gpu }, (group) => {
    for (const gpu of metrics.gpus ?? []) {
      addChartSection(group, {
        id: metricSectionId(`${gpu.gpu}-${gpu.deviceId}`),
        sectionKind: "GPU",
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
              sectionKind: "ZFS pool",
              title: pool.pool,
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
