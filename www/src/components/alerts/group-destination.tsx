import { Webhook } from "lucide-react"

import { SimpleTooltip } from "@/components/simple-tooltip"
import type { AlertGroup } from "@/lib/api/user/alerts"

/** Where a group's alerts are sent, or a warning when they go nowhere. */
function GroupDestination({ group }: { group: AlertGroup }) {
  if (!group.destinationName) {
    return (
      <SimpleTooltip content="This group's alerts are recorded in history but notify no one.">
        <span className="tooltip-hint text-warning">Nobody</span>
      </SimpleTooltip>
    )
  }

  return (
    <span className="flex items-center gap-1.5">
      <Webhook aria-hidden className="size-3.5 text-muted-foreground" />
      <span className="truncate">{group.destinationName}</span>
    </span>
  )
}

export { GroupDestination }
