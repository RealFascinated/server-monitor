import { apiFetch } from "@/lib/auth/api"
import { metricTimeWindowToEpochWindow } from "@/lib/metrics/time-window"
import type { MetricTimeWindow } from "@/lib/metrics/time-window"

export type { MetricTimeRange } from "@/lib/metrics/range"

/** Value array aligned to the response-level `timestamps` array. */
export type MetricValues = (number | null)[] | null | undefined

export type HostMetrics = {
  cpuUsage?: MetricValues
  memUsage?: MetricValues
  memAvailable?: MetricValues
  memTotal?: MetricValues
  load1?: MetricValues
  load5?: MetricValues
  load15?: MetricValues
  cpuUserPct?: MetricValues
  cpuSystemPct?: MetricValues
  cpuIowaitPct?: MetricValues
  cpuStealPct?: MetricValues
  memBuffers?: MetricValues
  memCached?: MetricValues
  swapUsed?: MetricValues
  swapTotal?: MetricValues
  processCount?: MetricValues
  runningProcesses?: MetricValues
  ctxSwitchesPerSecond?: MetricValues
  interruptsPerSecond?: MetricValues
  fdOpen?: MetricValues
  fdMax?: MetricValues
  fdUsagePct?: MetricValues
  oomKillsTotal?: MetricValues
  oomKillsPerSecond?: MetricValues
  cpuClockMhz?: MetricValues
  cpuPowerWatts?: MetricValues
}

export type CpuCoreMetrics = {
  cpu: string
  cpuCorePct?: MetricValues
}

export type DiskMetrics = {
  disk: string
  usagePercent?: MetricValues
  usedBytes?: MetricValues
  totalBytes?: MetricValues
  ioReadBps?: MetricValues
  ioWriteBps?: MetricValues
  ioUsagePct?: MetricValues
  ioWaitMs?: MetricValues
  inodeUsed?: MetricValues
  inodeTotal?: MetricValues
  readIops?: MetricValues
  writeIops?: MetricValues
  readLatencyMs?: MetricValues
  writeLatencyMs?: MetricValues
  etaUntilFull?: MetricValues
}

export type NetworkMetrics = {
  interface: string
  rxBps?: MetricValues
  txBps?: MetricValues
  rxPacketsPerSecond?: MetricValues
  txPacketsPerSecond?: MetricValues
  rxErrorsPerSecond?: MetricValues
  txErrorsPerSecond?: MetricValues
}

export type GpuMetrics = {
  gpu: string
  deviceId: string
  vendor: string
  usagePercent?: MetricValues
  encoderUsagePercent?: MetricValues
  decoderUsagePercent?: MetricValues
  memoryUsedBytes?: MetricValues
  memoryTotalBytes?: MetricValues
  temperatureCelsius?: MetricValues
  powerWatts?: MetricValues
}

export type ContainerMetrics = {
  container: string
  cpuUsage?: MetricValues
  memoryUsage?: MetricValues
}

export type TemperatureMetrics = {
  sensor: string
  temperatureCelsius?: MetricValues
}

export type ZfsArcMetrics = {
  sizeBytes?: MetricValues
  targetBytes?: MetricValues
  maxBytes?: MetricValues
  minBytes?: MetricValues
  dataBytes?: MetricValues
  metadataBytes?: MetricValues
  l2arcSizeBytes?: MetricValues
  hitRatio?: MetricValues
  missesPerSecond?: MetricValues
}

export type ZfsPoolMetrics = {
  pool: string
  health: string
  scanState: string
  capacityPercent?: MetricValues
  allocatedBytes?: MetricValues
  freeBytes?: MetricValues
  totalBytes?: MetricValues
  fragmentationPercent?: MetricValues
  scanPercent?: MetricValues
  readBps?: MetricValues
  writeBps?: MetricValues
  readIops?: MetricValues
  writeIops?: MetricValues
  checksumErrors?: MetricValues
}

export type TcpConnectionMetrics = {
  state: string
  connections?: MetricValues
}

export type ServerMetricsResponse = {
  id: number
  from: number
  to: number
  step: number | null
  timestamps: number[] | null
  host?: HostMetrics | null
  cpuCores?: CpuCoreMetrics[] | null
  disks?: DiskMetrics[] | null
  networks?: NetworkMetrics[] | null
  gpus?: GpuMetrics[] | null
  containers?: ContainerMetrics[] | null
  temperatures?: TemperatureMetrics[] | null
  zfsArc?: ZfsArcMetrics | null
  zfsPools?: ZfsPoolMetrics[] | null
  tcpConnections?: TcpConnectionMetrics[] | null
}

export function getUserServerMetrics(
  serverId: number,
  window: MetricTimeWindow
): Promise<ServerMetricsResponse> {
  const { from, to } = metricTimeWindowToEpochWindow(window)
  const params = new URLSearchParams({
    from: String(from),
    to: String(to),
  })
  return apiFetch<ServerMetricsResponse>(
    `/v1/user/servers/${serverId}/metrics?${params}`
  )
}
