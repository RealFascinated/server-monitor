import { Link } from "@tanstack/react-router"
import { Mail, Server, User as UserIcon } from "lucide-react"
import { memo } from "react"

import { CountBadge } from "@/components/count-badge"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { useUserInvites } from "@/hooks/use-user-invites"
import { cn } from "@/lib/utils"

const navItems = [
  {
    to: "/" as const,
    label: "Servers",
    icon: Server,
    exact: true,
  },
  {
    to: "/invites" as const,
    label: "Invites",
    icon: Mail,
    exact: true,
  },
  {
    to: "/settings" as const,
    label: "Account",
    icon: UserIcon,
    exact: true,
  },
] as const

function SidebarNavLink({
  compact,
  onNavigate,
  to,
  icon: Icon,
  label,
  exact,
  badgeCount = 0,
}: {
  compact: boolean
  onNavigate?: () => void
  to: "/" | "/invites" | "/settings"
  icon: typeof Server
  label: string
  exact: boolean
  badgeCount?: number
}) {
  const tooltip = badgeCount > 0 ? `${label} (${badgeCount})` : label
  const ariaLabel = badgeCount > 0 ? `${label}, ${badgeCount} pending` : label

  const link = (
    <Link
      to={to}
      onClick={onNavigate}
      activeOptions={{ exact }}
      aria-label={compact ? ariaLabel : undefined}
      className={cn(
        "flex min-h-7 w-full items-center gap-3 rounded-sm px-2 py-1 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted",
        "[&.active]:bg-muted [&.active]:text-foreground dark:[&.active]:text-warning",
        compact && "justify-center px-0",
        compact && "cursor-pointer"
      )}
    >
      <span className="relative shrink-0">
        <Icon className="size-4" />
        {compact ? (
          <CountBadge count={badgeCount} variant="accent" compact />
        ) : null}
      </span>
      {!compact ? <span className="truncate">{label}</span> : null}
      {!compact ? (
        <CountBadge count={badgeCount} variant="accent" className="ml-auto" />
      ) : null}
    </Link>
  )

  if (compact) {
    return <SimpleTooltip content={tooltip}>{link}</SimpleTooltip>
  }

  return link
}

export const SidebarNav = memo(function SidebarNav({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  const { data: invites = [] } = useUserInvites()
  const pendingInviteCount = invites.length

  return (
    <>
      {navItems.map(({ to, label, icon, exact }) => (
        <SidebarNavLink
          key={to}
          compact={compact}
          onNavigate={onNavigate}
          to={to}
          icon={icon}
          label={label}
          exact={exact}
          badgeCount={to === "/invites" ? pendingInviteCount : 0}
        />
      ))}
    </>
  )
})
