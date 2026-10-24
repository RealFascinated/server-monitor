import type { LucideIcon } from "lucide-react"
import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

type EmptyStateProps = {
  icon: LucideIcon
  title: string
  description?: string
  action?: ReactNode
  className?: string
}

function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center gap-3 rounded-lg border border-dashed border-border bg-card px-6 py-10 text-center",
        className
      )}
    >
      <span
        aria-hidden
        className="flex size-10 items-center justify-center rounded-full bg-primary/10 text-brand"
      >
        <Icon className="size-5" />
      </span>
      <div className="flex flex-col gap-1">
        <p className="text-sm font-semibold text-foreground">{title}</p>
        {description ? (
          <p className="mx-auto max-w-sm text-sm text-muted-foreground">
            {description}
          </p>
        ) : null}
      </div>
      {action}
    </div>
  )
}

export { EmptyState }
