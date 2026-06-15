import type { MetricValues } from "@/lib/api/user/metrics"

export type MetricsTimeGrid = {
  gridTimestamps: number[]
  sourceTimestamps: number[] | null
}

function buildWindowTimestamps(
  from: number,
  to: number,
  stepSeconds: number
): number[] {
  const alignedStart = from - (from % stepSeconds)
  const timestamps: number[] = []

  for (
    let timestamp = alignedStart;
    timestamp <= to;
    timestamp += stepSeconds
  ) {
    timestamps.push(timestamp)
  }

  return timestamps
}

function normalizeValue(value: number | null | undefined): number | null {
  if (value == null || !Number.isFinite(value)) {
    return null
  }

  return value
}

function isDenseGrid(apiTimestamps: number[], fullGrid: number[]): boolean {
  if (apiTimestamps.length === 0 || fullGrid.length === 0) {
    return false
  }

  return apiTimestamps.length >= fullGrid.length * 0.9
}

export type MetricsTimeSeriesEnvelope = {
  from: number
  to: number
  step: number | null
  timestamps: number[] | null
}

export function buildMetricsTimeGrid(
  metrics: MetricsTimeSeriesEnvelope
): MetricsTimeGrid {
  const step = metrics.step && metrics.step > 0 ? metrics.step : 300
  const fullGrid = buildWindowTimestamps(metrics.from, metrics.to, step)
  const apiTimestamps = metrics.timestamps

  if (apiTimestamps && apiTimestamps.length > 0) {
    const dense = isDenseGrid(apiTimestamps, fullGrid)

    return {
      gridTimestamps: dense ? apiTimestamps : fullGrid,
      sourceTimestamps: apiTimestamps,
    }
  }

  return {
    gridTimestamps: fullGrid,
    sourceTimestamps: null,
  }
}

export function alignValuesToTimestamps(
  gridTimestamps: number[],
  sourceTimestamps: number[] | null,
  values: MetricValues
): (number | null)[] | null {
  if (!values || values.length === 0) {
    return null
  }

  if (values.length === gridTimestamps.length) {
    return values.map((value) => normalizeValue(value))
  }

  if (
    sourceTimestamps &&
    sourceTimestamps.length === values.length &&
    sourceTimestamps.length > 0
  ) {
    const byTimestamp = new Map<number, number | null>()

    for (let index = 0; index < values.length; index++) {
      byTimestamp.set(sourceTimestamps[index], normalizeValue(values[index]))
    }

    return gridTimestamps.map((timestamp) => byTimestamp.get(timestamp) ?? null)
  }

  if (values.length < gridTimestamps.length) {
    const padding = gridTimestamps.length - values.length

    return [
      ...Array.from({ length: padding }, () => null),
      ...values.map((value) => normalizeValue(value)),
    ]
  }

  return values
    .slice(values.length - gridTimestamps.length)
    .map((value) => normalizeValue(value))
}
