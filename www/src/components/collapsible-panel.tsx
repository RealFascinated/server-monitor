import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

type CollapsiblePanelProps = {
  open: boolean
  children: ReactNode
  className?: string
}

function CollapsiblePanel({
  open,
  children,
  className,
}: CollapsiblePanelProps) {
  return (
    <div
      className={cn("motion-collapsible", open && "motion-collapsible-open")}
    >
      <div className={cn("motion-collapsible-inner", className)}>
        {children}
      </div>
    </div>
  )
}

export { CollapsiblePanel }
