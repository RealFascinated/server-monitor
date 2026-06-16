import { Link } from "@tanstack/react-router"
import { Gauge, Settings } from "lucide-react"
import { memo } from "react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import type { MetricTimeRange } from "@/lib/metrics/range"
import { cn } from "@/lib/utils"

function SidebarAdminLink({
  compact,
  onNavigate,
  to,
  search,
  icon: Icon,
  label,
}: {
  compact: boolean
  onNavigate?: () => void
  to: "/admin/metrics" | "/admin/settings"
  search?: { range: MetricTimeRange }
  icon: typeof Gauge
  label: string
}) {
  const link = (
    <Link
      to={to}
      search={search}
      onClick={onNavigate}
      className={cn(
        "flex min-h-7 w-full items-center gap-3 rounded-sm px-2 py-1 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted",
        "[&.active]:bg-muted [&.active]:text-foreground dark:[&.active]:text-warning",
        compact && "justify-center px-0",
        compact && "cursor-pointer"
      )}
    >
      <Icon className="size-4 shrink-0" />
      {!compact ? <span className="truncate">{label}</span> : null}
    </Link>
  )

  if (compact) {
    return <SimpleTooltip content={label}>{link}</SimpleTooltip>
  }

  return link
}

export const SidebarAdminSection = memo(function SidebarAdminSection({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  const { defaultRange } = useMetricDefaultRange()

  return (
    <div className="flex shrink-0 flex-col">
      {!compact ? (
        <p className="mt-3 mb-1 px-2 text-xs font-medium tracking-wide text-muted-foreground uppercase">
          Admin
        </p>
      ) : (
        <div className="my-2 shrink-0 border-t border-sidebar-border" />
      )}

      <SidebarAdminLink
        compact={compact}
        onNavigate={onNavigate}
        to="/admin/settings"
        icon={Settings}
        label="Settings"
      />
      <SidebarAdminLink
        compact={compact}
        onNavigate={onNavigate}
        to="/admin/metrics"
        search={{ range: defaultRange }}
        icon={Gauge}
        label="Metrics"
      />
    </div>
  )
})
