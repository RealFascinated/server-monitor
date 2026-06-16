import { cn } from "@/lib/utils"

type CountBadgeProps = {
  count: number
  variant?: "accent" | "muted"
  compact?: boolean
  hideZero?: boolean
  className?: string
}

function CountBadge({
  count,
  variant = "muted",
  compact = false,
  hideZero = variant === "accent",
  className,
}: CountBadgeProps) {
  if (hideZero && count < 1) {
    return null
  }

  const label = count > 99 ? "99+" : String(count)
  const compactLabel = count > 9 ? "9+" : label

  return (
    <span
      aria-hidden
      className={cn(
        "flex shrink-0 items-center justify-center rounded-full leading-none tabular-nums",
        compact
          ? "absolute -top-1 -right-1.5 h-4 min-w-4 px-0.5 text-[9px]"
          : "h-5 min-w-5 px-1 text-[10px]",
        variant === "accent"
          ? "bg-monitor font-semibold text-white dark:bg-warning dark:text-black"
          : "bg-muted font-medium text-muted-foreground",
        className
      )}
    >
      {compact ? compactLabel : label}
    </span>
  )
}

export { CountBadge }
