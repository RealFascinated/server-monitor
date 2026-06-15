import * as React from "react"
import { cva } from "class-variance-authority"
import type { VariantProps } from "class-variance-authority"
import { Slot } from "radix-ui"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
  "group/button inline-flex min-w-fit shrink-0 cursor-pointer items-center justify-center gap-2 rounded-sm border-2 border-transparent bg-clip-padding px-2 text-sm font-medium whitespace-nowrap normal-case transition-colors outline-none select-none focus-visible:ring-2 focus-visible:ring-monitor focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none disabled:pointer-events-none disabled:cursor-not-allowed disabled:border-neutral-300 disabled:bg-neutral-100 disabled:text-neutral-600 disabled:opacity-100 aria-disabled:pointer-events-none aria-disabled:cursor-not-allowed aria-disabled:border-neutral-300 aria-disabled:bg-neutral-100 aria-disabled:text-neutral-600 aria-disabled:opacity-100 dark:focus-visible:ring-warning dark:focus-visible:ring-offset-base dark:disabled:border-monitor-gray-300 dark:disabled:bg-monitor-gray-100/60 dark:disabled:text-neutral-400 dark:aria-disabled:border-monitor-gray-300 dark:aria-disabled:bg-monitor-gray-100/60 dark:aria-disabled:text-neutral-400 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default:
          "border-neutral-200 bg-white text-black hover:bg-neutral-100 dark:border-monitor-gray-300 dark:bg-monitor-gray-100 dark:text-white dark:hover:bg-monitor-gray-200",
        highlighted:
          "border-monitor bg-monitor-50 text-monitor-200 hover:bg-monitor hover:text-white dark:border-monitor-100 dark:bg-monitor/20 dark:text-white dark:hover:bg-monitor-100 dark:hover:text-white",
        brand:
          "border-monitor bg-monitor text-white hover:bg-monitor-100 dark:border-monitor-100 dark:bg-monitor/90 dark:hover:bg-monitor-100",
        destructive:
          "border-red-300 bg-red-50 text-red-800 hover:bg-error hover:text-white dark:border-red-800 dark:bg-red-900/30 dark:text-red-300 dark:hover:bg-red-800 dark:hover:text-white",
        outline:
          "border-neutral-200 bg-transparent text-black hover:bg-neutral-100 dark:border-monitor-gray-300 dark:text-white dark:hover:bg-monitor-gray-200",
        secondary:
          "border-neutral-200 bg-neutral-100 text-black hover:bg-neutral-200 dark:border-monitor-gray-300 dark:bg-monitor-gray-200 dark:text-white dark:hover:bg-monitor-gray-300",
        ghost:
          "border-transparent text-black hover:bg-neutral-100 dark:text-white dark:hover:bg-monitor-gray-200",
        link: "h-auto border-transparent bg-transparent p-0 text-monitor underline-offset-4 hover:underline dark:text-warning",
      },
      size: {
        default: "h-8 has-[>svg]:px-2",
        xs: "h-6 gap-1 px-2 text-xs has-[>svg]:px-2 [&_svg:not([class*='size-'])]:size-3",
        sm: "h-8 px-2 text-sm has-[>svg]:px-2 [&_svg:not([class*='size-'])]:size-3.5",
        lg: "h-10 px-3 has-[>svg]:px-3",
        icon: "size-8",
        "icon-xs": "size-6 [&_svg:not([class*='size-'])]:size-3",
        "icon-sm": "size-8",
        "icon-lg": "size-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : "button"

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
