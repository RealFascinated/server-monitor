import type { AlertSeverity } from "@/lib/api/user/alerts"
import { SEVERITY_LABELS } from "@/lib/alerts/summary"
import { cn } from "@/lib/utils"

function AlertSeverityBadge({ severity }: { severity: AlertSeverity }) {
  return (
    <span
      className={cn(
        "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium",
        severity === "CRITICAL"
          ? "bg-error/10 text-error"
          : "bg-warning/10 text-warning"
      )}
    >
      {SEVERITY_LABELS[severity]}
    </span>
  )
}

export { AlertSeverityBadge }
