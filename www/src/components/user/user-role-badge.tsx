import type { UserRole } from "@/lib/auth/types"
import { cn } from "@/lib/utils"

const roleLabels: Record<UserRole, string> = {
  ADMIN: "Admin",
  USER: "User",
}

const roleStyles: Record<UserRole, string> = {
  ADMIN:
    "bg-monitor/10 text-monitor ring-monitor/20 dark:bg-warning/10 dark:text-warning dark:ring-warning/20",
  USER: "bg-muted text-muted-foreground ring-border",
}

function UserRoleBadge({ role }: { role: UserRole }) {
  return (
    <span
      className={cn(
        "inline-flex w-fit items-center rounded-sm px-2 py-0.5 text-xs font-semibold tracking-wide uppercase ring-1 ring-inset",
        roleStyles[role]
      )}
    >
      {roleLabels[role]}
    </span>
  )
}

export { UserRoleBadge }
