import type {
  DiskMetrics,
  GpuMetrics,
  MetricValues,
  NetworkMetrics,
  ServerMetricsResponse,
  TemperatureMetrics,
  TcpConnectionMetrics,
  ZfsPoolMetrics,
} from "@/lib/api/user/metrics"
import {
  BATTERY_THRESHOLDS,
  PERCENT_THRESHOLDS,
  TEMPERATURE_THRESHOLDS,
} from "@/lib/metrics/chart-thresholds"
import type { ChartThreshold } from "@/lib/metrics/chart-thresholds"
import type {
  MetricChartConfig,
  TooltipSortEntry,
} from "@/lib/metrics/chart-config"
import { chartSeries } from "@/lib/metrics/series"
import {
  formatCelsius,
  formatCount,
  formatDurationSeconds,
  formatMegahertz,
  formatMilliseconds,
  formatMemoryBytes,
  formatNetworkRate,
  formatNumber,
  formatPercentValue,
  formatRate,
  formatWatts,
} from "@/lib/formatter"

const PERCENT_Y_RANGE = { max: 100 } as const

const PERCENT_RIGHT_AXIS = {
  rightYRange: PERCENT_Y_RANGE,
  rightValueFormatter: formatPercentValue,
} as const

function percentSeries(label: string, values: MetricValues) {
  return chartSeries(label, values, { axis: "right" })
}

function loadAverageThresholds(
  coreCount: number
): ChartThreshold[] | undefined {
  if (coreCount < 1) {
    return undefined
  }

  return [
    { value: coreCount, level: "warning" },
    { value: coreCount * 2, level: "critical" },
  ]
}

function cpuCoreIndex(label: string): number {
  const match = /\d+$/.exec(label)
  return match ? Number(match[0]) : 0
}

function compareCpuCoreLabels(
  a: TooltipSortEntry,
  b: TooltipSortEntry
): number {
  return cpuCoreIndex(a.label) - cpuCoreIndex(b.label)
}

function diskCharts(disk: DiskMetrics): MetricChartConfig[] {
  return [
    {
      title: "Usage",
      description: "Filesystem space used as a percentage of total capacity.",
      series: [chartSeries("Usage", disk.usagePercent)],
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: PERCENT_THRESHOLDS,
    },
    {
      title: "Capacity",
      description: "Used and total disk space over time.",
      series: [
        chartSeries("Used", disk.usedBytes),
        chartSeries("Total", disk.totalBytes),
      ],
      valueFormatter: formatMemoryBytes,
    },
    {
      title: "ETA until full",
      description:
        "Estimated time until the disk fills, based on recent growth.",
      series: [chartSeries("ETA", disk.etaUntilFull)],
      valueFormatter: formatDurationSeconds,
    },
    {
      title: "Throughput",
      description:
        "Disk read and write bytes per second. I/O utilization is on the right axis.",
      series: [
        chartSeries("Read", disk.ioReadBps),
        chartSeries("Write", disk.ioWriteBps, { negate: true }),
        percentSeries("I/O usage", disk.ioUsagePct),
      ],
      valueFormatter: formatRate,
      seriesFormatters: [formatRate, formatRate, formatPercentValue],
      ...PERCENT_RIGHT_AXIS,
    },
    {
      title: "I/O wait",
      description: "Average I/O wait time in milliseconds.",
      series: [chartSeries("Wait", disk.ioWaitMs)],
      valueFormatter: formatMilliseconds,
    },
    {
      title: "Inodes",
      description:
        "File metadata slots used vs total. Exhaustion blocks new files even with free space.",
      series: [
        chartSeries("Used", disk.inodeUsed),
        chartSeries("Total", disk.inodeTotal),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "IOPS",
      description: "Read and write operations per second.",
      series: [
        chartSeries("Read", disk.readIops),
        chartSeries("Write", disk.writeIops),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "Latency",
      description: "Average read and write latency in milliseconds.",
      series: [
        chartSeries("Read", disk.readLatencyMs),
        chartSeries("Write", disk.writeLatencyMs),
      ],
      valueFormatter: formatMilliseconds,
    },
  ]
}

function networkCharts(network: NetworkMetrics): MetricChartConfig[] {
  return [
    {
      title: "Throughput",
      description: "Receive and transmit bandwidth.",
      series: [
        chartSeries("RX", network.rxBps),
        chartSeries("TX", network.txBps, { negate: true }),
      ],
      valueFormatter: formatNetworkRate,
    },
    {
      title: "Packets",
      description: "Packets received and transmitted per second.",
      series: [
        chartSeries("RX", network.rxPacketsPerSecond),
        chartSeries("TX", network.txPacketsPerSecond),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "Errors",
      description: "Network errors per second on receive and transmit.",
      series: [
        chartSeries("RX", network.rxErrorsPerSecond),
        chartSeries("TX", network.txErrorsPerSecond),
      ],
      valueFormatter: formatCount,
    },
  ]
}

function gpuCharts(gpu: GpuMetrics): MetricChartConfig[] {
  return [
    {
      title: "Encode/decode",
      description:
        "Video encoder and decoder utilization on supported GPUs (for example NVENC/NVDEC on NVIDIA).",
      series: [
        chartSeries("Encoder", gpu.encoderUsagePercent),
        chartSeries("Decoder", gpu.decoderUsagePercent),
      ],
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: PERCENT_THRESHOLDS,
    },
    {
      title: "Memory",
      description:
        "GPU memory used vs total. Core utilization is on the right axis.",
      series: [
        chartSeries("Used", gpu.memoryUsedBytes),
        chartSeries("Total", gpu.memoryTotalBytes),
        percentSeries("Usage", gpu.usagePercent),
      ],
      valueFormatter: formatMemoryBytes,
      seriesFormatters: [
        formatMemoryBytes,
        formatMemoryBytes,
        formatPercentValue,
      ],
      ...PERCENT_RIGHT_AXIS,
    },
    {
      title: "Temperature",
      description: "GPU die temperature.",
      series: [chartSeries("°C", gpu.temperatureCelsius)],
      valueFormatter: formatCelsius,
      thresholds: TEMPERATURE_THRESHOLDS,
    },
    {
      title: "Power",
      description: "GPU power draw in watts.",
      series: [chartSeries("Watts", gpu.powerWatts)],
      valueFormatter: formatWatts,
    },
  ]
}

function containerCharts(
  containers: ServerMetricsResponse["containers"]
): MetricChartConfig[] {
  const items = containers ?? []

  return [
    {
      title: "CPU",
      description:
        "Per-container CPU share stacked. Hover the chart for values at a point in time.",
      mode: "stack",
      series: items.map((container) =>
        chartSeries(container.container, container.cpuUsage)
      ),
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: PERCENT_THRESHOLDS,
      showCurrentValues: false,
    },
    {
      title: "Memory",
      description:
        "Per-container memory usage stacked. Hover the chart for values at a point in time.",
      mode: "stack",
      series: items.map((container) =>
        chartSeries(container.container, container.memoryUsage)
      ),
      valueFormatter: formatMemoryBytes,
      showCurrentValues: false,
    },
  ]
}

function zfsPoolCharts(pool: ZfsPoolMetrics): MetricChartConfig[] {
  return [
    {
      title: "Capacity",
      description:
        "Pool capacity, fragmentation, and scrub/resilver scan progress.",
      series: [
        chartSeries("Capacity", pool.capacityPercent),
        chartSeries("Fragmentation", pool.fragmentationPercent),
        chartSeries("Scan", pool.scanPercent),
      ],
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: PERCENT_THRESHOLDS,
    },
    {
      title: "Space",
      description: "Allocated, free, and total pool space.",
      series: [
        chartSeries("Allocated", pool.allocatedBytes),
        chartSeries("Free", pool.freeBytes),
        chartSeries("Total", pool.totalBytes),
      ],
      valueFormatter: formatMemoryBytes,
    },
    {
      title: "I/O",
      description: "Pool read and write throughput.",
      series: [
        chartSeries("Read", pool.readBps),
        chartSeries("Write", pool.writeBps),
      ],
      valueFormatter: formatRate,
    },
    {
      title: "IOPS",
      description: "Pool read and write operations per second.",
      series: [
        chartSeries("Read", pool.readIops),
        chartSeries("Write", pool.writeIops),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "Checksum errors",
      description: "ZFS checksum errors detected on this pool.",
      series: [chartSeries("Errors", pool.checksumErrors)],
      valueFormatter: formatCount,
    },
  ]
}

function hostCpuCharts(
  host: NonNullable<ServerMetricsResponse["host"]>,
  cpuCores: ServerMetricsResponse["cpuCores"]
): MetricChartConfig[] {
  return [
    {
      title: "CPU breakdown",
      description:
        "User, system, I/O wait, and steal time stacked as a share of CPU capacity. I/O wait is time spent waiting on disk. Steal is time taken by the hypervisor on VMs.",
      mode: "stack",
      series: [
        chartSeries("User", host.cpuUserPct),
        chartSeries("System", host.cpuSystemPct),
        chartSeries("IO wait", host.cpuIowaitPct),
        chartSeries("Steal", host.cpuStealPct),
      ],
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: PERCENT_THRESHOLDS,
    },
    {
      title: "Load average",
      description:
        "Runnable and blocked tasks averaged over 1, 5, and 15 minutes. Compare to logical CPU count — sustained load near or above core count indicates saturation.",
      series: [
        chartSeries("1 min", host.load1),
        chartSeries("5 min", host.load5),
        chartSeries("15 min", host.load15),
      ],
      valueFormatter: formatNumber,
      thresholds: loadAverageThresholds(cpuCores?.length ?? 0),
    },
    {
      title: "Per-core usage",
      description: "Utilization per logical CPU core.",
      series: (cpuCores ?? []).map((core) =>
        chartSeries(core.cpu, core.cpuCorePct)
      ),
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: PERCENT_THRESHOLDS,
      showCurrentValues: false,
      tooltipColumnSize: 16,
      tooltipSort: compareCpuCoreLabels,
    },
    {
      title: "CPU clock",
      description: "Current CPU frequency.",
      series: [chartSeries("MHz", host.cpuClockMhz)],
      valueFormatter: formatMegahertz,
    },
    {
      title: "CPU power",
      description: "Processor power draw in watts.",
      series: [chartSeries("Watts", host.cpuPowerWatts)],
      valueFormatter: formatWatts,
    },
  ]
}

function hostMemoryCharts(
  host: NonNullable<ServerMetricsResponse["host"]>
): MetricChartConfig[] {
  return [
    {
      title: "Physical memory",
      description: "Used, available, and total RAM.",
      series: [
        chartSeries("Used", host.memUsage),
        chartSeries("Available", host.memAvailable),
        chartSeries("Total", host.memTotal),
      ],
      valueFormatter: formatMemoryBytes,
    },
    {
      title: "Buffers & cache",
      description: "Kernel buffers and page cache.",
      series: [
        chartSeries("Buffers", host.memBuffers),
        chartSeries("Cached", host.memCached),
      ],
      valueFormatter: formatMemoryBytes,
    },
    {
      title: "Swap",
      description: "Swap space used vs total.",
      series: [
        chartSeries("Used", host.swapUsed),
        chartSeries("Total", host.swapTotal),
      ],
      valueFormatter: formatMemoryBytes,
    },
    {
      title: "OOM kills",
      description:
        "Out-of-memory killer events. Total is cumulative; rate is kills per second.",
      series: [
        chartSeries("Total", host.oomKillsTotal),
        chartSeries("Kills/s", host.oomKillsPerSecond),
      ],
      valueFormatter: formatCount,
      seriesFormatters: [formatCount, (value) => `${formatCount(value)}/s`],
    },
  ]
}

function hostSystemCharts(
  host: NonNullable<ServerMetricsResponse["host"]>
): MetricChartConfig[] {
  return [
    {
      title: "Processes",
      description: "Total and currently running processes.",
      series: [
        chartSeries("Total", host.processCount),
        chartSeries("Running", host.runningProcesses),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "Scheduling",
      description: "Context switches and hardware interrupts per second.",
      series: [
        chartSeries("Context switches/s", host.ctxSwitchesPerSecond),
        chartSeries("Interrupts/s", host.interruptsPerSecond),
      ],
      valueFormatter: formatCount,
    },
    {
      title: "File descriptors",
      description:
        "Open file descriptors vs the kernel limit. High usage can block new connections and file opens.",
      series: [
        chartSeries("Open", host.fdOpen),
        chartSeries("Max", host.fdMax),
        percentSeries("Usage", host.fdUsagePct),
      ],
      valueFormatter: formatCount,
      seriesFormatters: [formatCount, formatCount, formatPercentValue],
      ...PERCENT_RIGHT_AXIS,
    },
  ]
}

function hostHardwareCharts(
  host: NonNullable<ServerMetricsResponse["host"]>,
  temperatures: TemperatureMetrics[] | null | undefined
): MetricChartConfig[] {
  return [
    {
      title: "Temperature",
      description: "Hardware temperature readings from system sensors.",
      series: (temperatures ?? []).map((sensor) =>
        chartSeries(sensor.sensor, sensor.temperatureCelsius)
      ),
      valueFormatter: formatCelsius,
      thresholds: TEMPERATURE_THRESHOLDS,
      showCurrentValues: false,
    },
    {
      title: "Battery",
      description: "Battery charge level on laptops and portable devices.",
      series: [chartSeries("Charge", host.batteryPct)],
      valueFormatter: formatPercentValue,
      yRange: PERCENT_Y_RANGE,
      thresholds: BATTERY_THRESHOLDS,
    },
  ]
}

function tcpCharts(
  tcpConnections: TcpConnectionMetrics[] | null | undefined
): MetricChartConfig[] {
  return [
    {
      title: "Connections by state",
      description:
        "TCP connections grouped by state (for example ESTABLISHED, TIME_WAIT).",
      series: (tcpConnections ?? []).map((tcp) =>
        chartSeries(tcp.state, tcp.connections)
      ),
      valueFormatter: formatCount,
      showCurrentValues: false,
    },
  ]
}

export type { MetricChartConfig } from "@/lib/metrics/chart-config"
export {
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
}
