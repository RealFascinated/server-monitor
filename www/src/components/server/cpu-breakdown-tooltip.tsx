import type { ServerCpuSnapshot } from "@/lib/api/user/servers"
import { formatPercentValue } from "@/lib/formatter"
import { getChartColor } from "@/lib/metrics/chart-colors"
import { useTheme } from "@/lib/theme"

const CPU_BREAKDOWN_FIELDS = [
  { key: "user", label: "User" },
  { key: "system", label: "System" },
  { key: "iowait", label: "IO wait" },
  { key: "steal", label: "Steal" },
] as const satisfies ReadonlyArray<{
  key: keyof Omit<ServerCpuSnapshot, "percent">
  label: string
}>

function hasCpuBreakdown(cpu: ServerCpuSnapshot): boolean {
  return CPU_BREAKDOWN_FIELDS.some(({ key }) => cpu[key] != null)
}

function CpuBreakdownTooltip({ cpu }: { cpu: ServerCpuSnapshot }) {
  const { resolvedTheme } = useTheme()
  const entries = CPU_BREAKDOWN_FIELDS.flatMap(({ key, label }, index) => {
    const value = cpu[key]
    if (value == null) {
      return []
    }

    return [{ label, value, color: getChartColor(index, resolvedTheme) }]
  })

  if (entries.length === 0) {
    return null
  }

  return (
    <div className="flex flex-col">
      {entries.map((entry) => (
        <div key={entry.label} className="flex items-center gap-2 py-0.5">
          <span
            className="size-2 shrink-0 rounded-full"
            style={{ background: entry.color }}
          />
          <span className="truncate text-muted-foreground dark:text-muted-foreground">
            {entry.label}
          </span>
          <span className="ml-auto pl-3 font-medium whitespace-nowrap tabular-nums">
            {formatPercentValue(entry.value)}
          </span>
        </div>
      ))}
      {cpu.percent != null && entries.length > 1 ? (
        <div className="mt-1 flex items-center justify-between border-t border-border pt-1">
          <span className="text-muted-foreground dark:text-muted-foreground">Total</span>
          <span className="font-medium tabular-nums">
            {formatPercentValue(cpu.percent)}
          </span>
        </div>
      ) : null}
    </div>
  )
}

export { CpuBreakdownTooltip, hasCpuBreakdown }
