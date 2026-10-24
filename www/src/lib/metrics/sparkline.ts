import type {
  MetricValues,
  ServerMetricsResponse,
} from "@/lib/api/user/metrics"

export type SparklineValues = (number | null)[]

export type ServerSparklines = {
  cpu: SparklineValues
  memory: SparklineValues
}

const DEFAULT_MAX_POINTS = 48

/** Averages values into at most `maxPoints` buckets, ignoring nulls. */
export function downsampleValues(
  values: SparklineValues,
  maxPoints = DEFAULT_MAX_POINTS
): SparklineValues {
  if (values.length <= maxPoints) {
    return values
  }

  const bucketSize = values.length / maxPoints
  const result: SparklineValues = []

  for (let bucket = 0; bucket < maxPoints; bucket++) {
    const start = Math.floor(bucket * bucketSize)
    const end = Math.max(start + 1, Math.floor((bucket + 1) * bucketSize))
    let sum = 0
    let count = 0

    for (let index = start; index < end && index < values.length; index++) {
      const value = values[index]
      if (value != null && Number.isFinite(value)) {
        sum += value
        count++
      }
    }

    result.push(count > 0 ? sum / count : null)
  }

  return result
}

function toPercentOfTotal(used: MetricValues, total: MetricValues) {
  if (!used || !total) {
    return []
  }

  return used.map((value, index) => {
    const max = total[index]
    if (value == null || max == null || max <= 0) {
      return null
    }

    return (value / max) * 100
  })
}

export function buildServerSparklines(
  metrics: ServerMetricsResponse
): ServerSparklines {
  const host = metrics.host

  return {
    cpu: downsampleValues([...(host?.cpuUsage ?? [])]),
    memory: downsampleValues(toPercentOfTotal(host?.memUsage, host?.memTotal)),
  }
}

export function sparklineHasData(values: SparklineValues | undefined): boolean {
  return values?.some((value) => value != null) ?? false
}

export type SparklineStats = {
  min: number
  max: number
  avg: number
  latest: number
}

export function sparklineStats(values: SparklineValues): SparklineStats | null {
  let min = Number.POSITIVE_INFINITY
  let max = Number.NEGATIVE_INFINITY
  let sum = 0
  let count = 0
  let latest: number | null = null

  for (const value of values) {
    if (value == null || !Number.isFinite(value)) {
      continue
    }

    min = Math.min(min, value)
    max = Math.max(max, value)
    sum += value
    count++
    latest = value
  }

  if (count === 0 || latest == null) {
    return null
  }

  return { min, max, avg: sum / count, latest }
}
