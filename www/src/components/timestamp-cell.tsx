import { SimpleTooltip } from "@/components/simple-tooltip"
import { formatDate, formatDateWithRelative } from "@/lib/formatter"
import { cn } from "@/lib/utils"

type TimestampCellProps = {
  iso: string
  tooltip?: string
  className?: string
}

function TimestampCell({ iso, tooltip, className }: TimestampCellProps) {
  return (
    <SimpleTooltip content={tooltip ?? formatDateWithRelative(iso)}>
      <span className={cn("cursor-help", className)}>{formatDate(iso)}</span>
    </SimpleTooltip>
  )
}

export { TimestampCell }
