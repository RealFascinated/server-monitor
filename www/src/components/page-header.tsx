import type { ReactNode } from "react"
import type { LucideIcon } from "lucide-react"

import { Breadcrumb } from "@/components/breadcrumb"
import type { BreadcrumbItem } from "@/components/breadcrumb"
import { cn } from "@/lib/utils"

type PageHeaderProps = {
  breadcrumb?: BreadcrumbItem[]
  icon?: LucideIcon
  title: string
  titleAddon?: ReactNode
  description?: string
  subtitle?: ReactNode
  actions?: ReactNode
  footer?: ReactNode
  sticky?: boolean
  className?: string
}

function PageHeader({
  breadcrumb,
  icon: Icon,
  title,
  titleAddon,
  description,
  subtitle,
  actions,
  footer,
  sticky = false,
  className,
}: PageHeaderProps) {
  return (
    <div
      className={cn(
        "mb-6 flex flex-col gap-2.5 border-b border-sidebar-border py-3",
        sticky && "z-30 bg-background/95 backdrop-blur-sm lg:sticky lg:top-0",
        className
      )}
    >
      {breadcrumb ? <Breadcrumb items={breadcrumb} /> : null}

      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className="flex min-w-0 flex-wrap items-center gap-2">
            {Icon ? (
              <Icon className="size-4 shrink-0 text-monitor dark:text-warning" />
            ) : null}
            <h1 className="text-xl">{title}</h1>
            {titleAddon}
          </div>

          {subtitle}
          {!subtitle && description ? (
            <p className="text-sm text-muted-foreground">{description}</p>
          ) : null}
        </div>

        {actions}
      </div>

      {footer}
    </div>
  )
}

export { PageHeader }
