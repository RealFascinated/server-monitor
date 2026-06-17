export function formatDurationSeconds(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "—"
  }

  const days = Math.floor(seconds / 86_400)
  if (days >= 365) {
    const years = Math.floor(days / 365)
    const remDays = days % 365
    return remDays > 0 ? `${years}y ${remDays}d` : `${years}y`
  }

  return formatUptime(seconds)
}

export function formatUptime(seconds: number | null): string {
  if (seconds == null) {
    return "—"
  }

  const days = Math.floor(seconds / 86_400)
  const hours = Math.floor((seconds % 86_400) / 3_600)
  const minutes = Math.floor((seconds % 3_600) / 60)

  if (days > 0) {
    return `${days}d ${hours}h`
  }

  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }

  return `${minutes}m`
}

export function formatPercent(value: number | null): string {
  if (value == null) {
    return "—"
  }

  return formatPercentValue(value)
}

function formatDecimal(value: number, maximumFractionDigits: number): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits,
    minimumFractionDigits: maximumFractionDigits,
  }).format(value)
}

export function formatPercentValue(value: number, fractionDigits = 1): string {
  return `${formatDecimal(value, fractionDigits)}%`
}

export function formatUptimePercent30d(value: number | null): string {
  if (value == null) {
    return "—"
  }

  return formatPercentValue(value, 2)
}

export function formatBytes(value: number, fractionDigits?: number): string {
  const units = ["B", "KB", "MB", "GB", "TB"]
  let size = value
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }

  const decimals = fractionDigits ?? (size >= 10 || unitIndex === 0 ? 0 : 1)
  return `${formatDecimal(size, decimals)} ${units[unitIndex]}`
}

export function formatMemoryBytes(value: number): string {
  return formatBytes(value, 2)
}

export function formatRate(value: number): string {
  return `${formatBytes(value)}/s`
}

export function formatNetworkRate(bytesPerSecond: number): string {
  const bitsPerSecond = bytesPerSecond * 8
  const units = ["bps", "Kbps", "Mbps", "Gbps", "Tbps"]
  let size = bitsPerSecond
  let unitIndex = 0

  while (size >= 1000 && unitIndex < units.length - 1) {
    size /= 1000
    unitIndex++
  }

  const decimals = size >= 10 || unitIndex === 0 ? 0 : 1
  return `${formatDecimal(size, decimals)} ${units[unitIndex]}`
}

export function formatCount(value: number): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatCompactCount(value: number, fractionDigits?: number): string {
  const units = ["", "K", "M", "B", "T"]
  let size = Math.abs(value)
  let unitIndex = 0

  while (size >= 1000 && unitIndex < units.length - 1) {
    size /= 1000
    unitIndex++
  }

  const sign = value < 0 ? "-" : ""
  const decimals = fractionDigits ?? (unitIndex === 0 ? 0 : 2)
  const formatted = new Intl.NumberFormat(undefined, {
    maximumFractionDigits: decimals,
  }).format(size)
  return `${sign}${formatted}${units[unitIndex]}`
}

export function formatPerMinute(value: number): string {
  return `${formatCount(value)}/min`
}

export function formatNumber(value: number): string {
  return formatDecimal(value, 1)
}

export function formatMegahertz(value: number): string {
  return `${formatNumber(value)} MHz`
}

export function formatWatts(value: number): string {
  return `${formatNumber(value)} W`
}

export function formatMilliseconds(value: number): string {
  return `${formatNumber(value)} ms`
}

export function formatCelsius(value: number): string {
  return `${formatNumber(value)} °C`
}

export function memoryUsagePercent(
  usage: number | null,
  max: number | null
): number | null {
  if (usage == null || max == null || max === 0) {
    return null
  }

  return (usage / max) * 100
}

export function formatMemoryUsage(
  usage: number | null,
  max: number | null
): string {
  return formatPercent(memoryUsagePercent(usage, max))
}

const RELATIVE_TIME_THRESHOLD_MS = 30 * 86_400 * 1000

function formatAbsoluteDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso))
}

function isWithinRelativeTimeThreshold(iso: string): boolean {
  return (
    Math.abs(new Date(iso).getTime() - Date.now()) < RELATIVE_TIME_THRESHOLD_MS
  )
}

export function formatServerRole(role: string): string {
  return role.charAt(0) + role.slice(1).toLowerCase()
}

export function formatDate(iso: string): string {
  if (isWithinRelativeTimeThreshold(iso)) {
    return formatRelativeTime(iso)
  }

  return formatAbsoluteDate(iso)
}

export function formatRelativeTime(iso: string): string {
  const date = new Date(iso)
  const diffMs = date.getTime() - Date.now()

  if (Math.abs(diffMs) < 60_000) {
    return "just now"
  }

  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" })
  const units: [Intl.RelativeTimeFormatUnit, number][] = [
    ["year", 365 * 86_400 * 1000],
    ["month", 30 * 86_400 * 1000],
    ["day", 86_400 * 1000],
    ["hour", 3_600 * 1000],
    ["minute", 60 * 1000],
  ]

  for (const [unit, ms] of units) {
    if (Math.abs(diffMs) >= ms || unit === "minute") {
      return rtf.format(Math.round(diffMs / ms), unit)
    }
  }

  return rtf.format(0, "second")
}

export function formatDateWithRelative(iso: string): string {
  const absolute = formatAbsoluteDate(iso)

  if (isWithinRelativeTimeThreshold(iso)) {
    return absolute
  }

  return `${absolute} (${formatRelativeTime(iso)})`
}

export function formatUptimeDetailed(seconds: number | null): string | null {
  if (seconds == null) {
    return null
  }

  const days = Math.floor(seconds / 86_400)
  const hours = Math.floor((seconds % 86_400) / 3_600)
  const minutes = Math.floor((seconds % 3_600) / 60)
  const parts: string[] = []

  if (days > 0) {
    parts.push(`${days} day${days === 1 ? "" : "s"}`)
  }

  if (hours > 0) {
    parts.push(`${hours} hour${hours === 1 ? "" : "s"}`)
  }

  if (minutes > 0 || parts.length === 0) {
    parts.push(`${minutes} minute${minutes === 1 ? "" : "s"}`)
  }

  return parts.join(", ")
}

const DAY_SECONDS = 86_400
const MONTH_SECONDS = 86_400 * 28
const YEAR_SECONDS = 86_400 * 365

export function formatChartAxisTicks(
  splits: number[],
  foundIncr: number
): string[] {
  let prevYear: number | undefined
  let prevDay: number | undefined

  const formatYear = (date: Date) =>
    new Intl.DateTimeFormat(undefined, { year: "numeric" }).format(date)

  const formatMonth = (date: Date) =>
    new Intl.DateTimeFormat(undefined, { month: "short" }).format(date)

  const formatMonthYear = (date: Date) =>
    new Intl.DateTimeFormat(undefined, {
      month: "short",
      year: "numeric",
    }).format(date)

  const formatMonthDay = (date: Date) =>
    new Intl.DateTimeFormat(undefined, {
      month: "short",
      day: "numeric",
    }).format(date)

  const formatMonthDayYear = (date: Date) =>
    new Intl.DateTimeFormat(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
    }).format(date)

  const formatTime = (date: Date) =>
    new Intl.DateTimeFormat(undefined, {
      hour: "numeric",
      minute: "2-digit",
      ...(foundIncr < 60 ? { second: "2-digit" } : {}),
    }).format(date)

  return splits.map((split) => {
    const date = new Date(split * 1000)
    const year = date.getFullYear()
    const day = date.getDate()

    const atYearBoundary = prevYear !== undefined && year !== prevYear
    const atDayBoundary = prevDay !== undefined && day !== prevDay

    prevYear = year
    prevDay = day

    if (foundIncr >= YEAR_SECONDS) {
      return formatYear(date)
    }

    if (foundIncr >= MONTH_SECONDS) {
      return atYearBoundary ? formatMonthYear(date) : formatMonth(date)
    }

    if (foundIncr >= DAY_SECONDS) {
      return atYearBoundary ? formatMonthDayYear(date) : formatMonthDay(date)
    }

    if (atDayBoundary) {
      return formatMonthDay(date)
    }

    return formatTime(date)
  })
}

export function formatCpuInventoryTooltip(
  inventory:
    | {
        socketCount?: number | null
        coreCount?: number | null
        threadCount?: number | null
      }
    | null
    | undefined
): string | null {
  if (!inventory) {
    return null
  }

  const { socketCount, coreCount, threadCount } = inventory
  const parts: string[] = []

  if (coreCount != null) {
    if (
      socketCount != null &&
      socketCount > 1 &&
      coreCount % socketCount === 0
    ) {
      parts.push(`${socketCount}×${coreCount / socketCount} cores`)
    } else {
      parts.push(`${coreCount} cores`)
    }
  }

  if (threadCount != null) {
    parts.push(`${threadCount} threads`)
  }

  return parts.length > 0 ? parts.join(" · ") : null
}

export function formatTooltipTimestamp(
  timestamp: number,
  rangeSeconds: number
): string {
  const date = new Date(timestamp * 1000)
  const now = new Date()
  const includeYear =
    date.getFullYear() !== now.getFullYear() || rangeSeconds > 86_400 * 180

  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    ...(includeYear ? { year: "numeric" } : {}),
    hour: "numeric",
    minute: "2-digit",
    ...(rangeSeconds <= 86_400 ? { second: "2-digit" } : {}),
  }).format(date)
}
