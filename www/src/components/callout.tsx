import type * as React from "react"
import { cva } from "class-variance-authority"
import type { VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const calloutVariants = cva(
  "relative flex flex-col gap-1 rounded-sm border p-3 text-sm",
  {
    variants: {
      type: {
        warning:
          "border-warning-300 bg-warning-50 dark:border-warning-800 dark:bg-warning-900/30",
        danger:
          "border-destructive/30 bg-destructive/10 dark:border-destructive/40 dark:bg-destructive/15",
        info: "border-chart-2/30 bg-chart-2/10 dark:border-chart-2/40 dark:bg-chart-2/15",
        success:
          "border-success/30 bg-success/10 dark:border-success/40 dark:bg-success/15",
      },
    },
    defaultVariants: {
      type: "info",
    },
  }
)

const titleVariants = cva("text-sm font-bold", {
  variants: {
    type: {
      warning: "text-warning-800 dark:text-warning-300",
      danger: "text-destructive",
      info: "text-chart-2",
      success: "text-success",
    },
  },
  defaultVariants: {
    type: "info",
  },
})

const bodyVariants = cva("text-sm", {
  variants: {
    type: {
      warning: "text-warning-700 dark:text-warning-200",
      danger: "text-destructive/90",
      info: "text-foreground",
      success: "text-foreground",
    },
  },
  defaultVariants: {
    type: "info",
  },
})

type CalloutProps = React.ComponentProps<"div"> &
  VariantProps<typeof calloutVariants> & {
    title: string
    children?: React.ReactNode
  }

function Callout({
  className,
  type = "info",
  title,
  children,
  ...props
}: CalloutProps) {
  return (
    <div
      className={cn(
        calloutVariants({ type }),
        "motion-callout",
        className
      )}
      {...props}
    >
      <p className={titleVariants({ type })}>{title}</p>
      {children ? (
        <div className={bodyVariants({ type })}>{children}</div>
      ) : null}
    </div>
  )
}

export { Callout }
