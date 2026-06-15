import type { MetricValues } from "@/lib/api/user/metrics"

function intervalSeconds(
  timestamps: number[] | null,
  index: number,
  fallbackStepSeconds: number
): number {
  if (timestamps && index > 0) {
    const delta = timestamps[index] - timestamps[index - 1]
    if (delta > 0) {
      return delta
    }
  }

  return fallbackStepSeconds
}

/**
 * Converts a Prometheus counter series into an events-per-minute rate.
 * Mirrors `rate(metric[window]) * 60` from the platform PromQL builder.
 */
export function counterRatePerMinute(
  values: MetricValues,
  timestamps: number[] | null,
  fallbackStepSeconds: number
): (number | null)[] | null {
  if (!values || values.length === 0) {
    return null
  }

  const result: (number | null)[] = []

  for (let index = 0; index < values.length; index++) {
    if (index === 0) {
      result.push(null)
      continue
    }

    const previous = values[index - 1]
    const current = values[index]

    if (
      previous == null ||
      current == null ||
      !Number.isFinite(previous) ||
      !Number.isFinite(current)
    ) {
      result.push(null)
      continue
    }

    const delta = current >= previous ? current - previous : current
    const seconds = intervalSeconds(timestamps, index, fallbackStepSeconds)
    result.push((delta / seconds) * 60)
  }

  return result
}

/**
 * Average over each histogram scrape interval: Δsum / Δcount.
 * Mirrors `rate(sum) / rate(count)` for histogram metrics.
 */
export function histogramIntervalAverage(
  sum: MetricValues,
  count: MetricValues
): (number | null)[] | null {
  if (!sum || !count || sum.length === 0 || count.length === 0) {
    return null
  }

  const length = Math.max(sum.length, count.length)
  const result: (number | null)[] = []

  for (let index = 0; index < length; index++) {
    if (index === 0) {
      result.push(null)
      continue
    }

    const previousSum = sum[index - 1]
    const currentSum = sum[index]
    const previousCount = count[index - 1]
    const currentCount = count[index]

    if (
      previousSum == null ||
      currentSum == null ||
      previousCount == null ||
      currentCount == null ||
      !Number.isFinite(previousSum) ||
      !Number.isFinite(currentSum) ||
      !Number.isFinite(previousCount) ||
      !Number.isFinite(currentCount)
    ) {
      result.push(null)
      continue
    }

    const sumDelta =
      currentSum >= previousSum ? currentSum - previousSum : currentSum
    const countDelta =
      currentCount >= previousCount
        ? currentCount - previousCount
        : currentCount

    if (countDelta <= 0) {
      result.push(null)
      continue
    }

    result.push(sumDelta / countDelta)
  }

  return result
}

export function resolveMetricStep(
  step: number | null | undefined,
  timestamps: number[] | null
): number {
  if (step && step > 0) {
    return step
  }

  if (timestamps && timestamps.length > 1) {
    const delta = timestamps[1] - timestamps[0]
    if (delta > 0) {
      return delta
    }
  }

  return 60
}
