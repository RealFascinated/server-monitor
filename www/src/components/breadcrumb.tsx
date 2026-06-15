import { Fragment } from "react"
import { Link } from "@tanstack/react-router"
import { ChevronRight } from "lucide-react"

import { cn } from "@/lib/utils"

type BreadcrumbLinkItem = {
  label: string
  current?: false
  to: string
  params?: Record<string, string>
  search?: Record<string, unknown>
}

type BreadcrumbCurrentItem = {
  label: string
  current: true
}

type BreadcrumbItem = BreadcrumbLinkItem | BreadcrumbCurrentItem

type BreadcrumbProps = {
  items: BreadcrumbItem[]
  className?: string
}

const breadcrumbLinkClassName =
  "shrink-0 rounded-sm text-muted-foreground transition-colors hover:text-monitor focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-monitor dark:hover:text-warning dark:focus-visible:ring-warning"

function Breadcrumb({ items, className }: BreadcrumbProps) {
  return (
    <nav
      aria-label="Breadcrumb"
      className={cn("min-w-0 overflow-x-auto", className)}
    >
      <ol className="flex flex-wrap items-center gap-1.5 text-sm font-medium">
        {items.map((item, index) => (
          <Fragment key={`${item.label}-${index}`}>
            {index > 0 ? (
              <li
                aria-hidden
                className="inline-flex items-center text-muted-foreground/60"
              >
                <ChevronRight className="size-3 shrink-0" />
              </li>
            ) : null}
            <li className="inline-flex items-center gap-1.5">
              {item.current ? (
                <span aria-current="page" className="shrink-0 text-foreground">
                  {item.label}
                </span>
              ) : (
                <Link
                  to={item.to}
                  params={item.params}
                  search={item.search}
                  className={breadcrumbLinkClassName}
                >
                  {item.label}
                </Link>
              )}
            </li>
          </Fragment>
        ))}
      </ol>
    </nav>
  )
}

export { Breadcrumb }
export type { BreadcrumbItem }
