import { SimpleTooltip } from "@/components/simple-tooltip"
import type { UserRole } from "@/lib/auth/types"
import { USER_ROLE_LABELS, USER_ROLE_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

const roleStyles: Record<UserRole, string> = {
  ADMIN:
    "bg-monitor/10 text-monitor ring-monitor/20 dark:bg-warning/10 dark:text-warning dark:ring-warning/20",
  USER: "bg-neutral-100 text-neutral-600 ring-neutral-200 dark:bg-monitor-gray-100 dark:text-neutral-300 dark:ring-monitor-gray-300",
}

function UserRoleBadge({ role }: { role: UserRole }) {
  return (
    <SimpleTooltip content={USER_ROLE_TOOLTIPS[role]}>
      <span
        className={cn(
          "inline-flex w-fit cursor-help items-center rounded-sm px-2 py-0.5 text-xs font-semibold tracking-wide uppercase ring-1 ring-inset",
          roleStyles[role]
        )}
      >
        {USER_ROLE_LABELS[role]}
      </span>
    </SimpleTooltip>
  )
}

export { UserRoleBadge }
