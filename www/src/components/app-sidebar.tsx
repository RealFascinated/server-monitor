import { Link } from "@tanstack/react-router"
import { ChevronLeft, LogOut, X } from "lucide-react"
import { memo, useCallback } from "react"
import type { PointerEvent as ReactPointerEvent } from "react"

import { MobileSidebarDrawer } from "@/components/sidebar/mobile-sidebar-drawer"
import { SidebarAdminSection } from "@/components/sidebar/sidebar-admin-section"
import { SidebarFolderTree } from "@/components/sidebar/sidebar-folder-tree"
import { SidebarNav } from "@/components/sidebar/sidebar-nav"
import { SidebarResizeHandle } from "@/components/sidebar/sidebar-resize-handle"
import { MonitorLogo } from "@/components/monitor-logo"
import { Spinner } from "@/components/spinner"
import { ThemeSwitcher } from "@/components/theme-switcher"
import { Button } from "@/components/ui/button"
import type { User } from "@/lib/auth/types"
import { APP_NAME } from "@/lib/page-title"
import { cn } from "@/lib/utils"

type AppSidebarProps = {
  user: User
  width: number
  compact: boolean
  collapsed: boolean
  isResizing: boolean
  mobileOpen: boolean
  onToggleCollapsed: () => void
  onResizeStart: (event: ReactPointerEvent<HTMLDivElement>) => void
  onMobileClose: () => void
  isLoggingOut: boolean
  onLogout: () => void
}

const AppSidebarHeader = memo(function AppSidebarHeader({
  compact,
  onNavigate,
  onMobileClose,
}: {
  compact: boolean
  onNavigate: () => void
  onMobileClose: () => void
}) {
  return (
    <div
      className={cn(
        "flex items-center gap-2 p-4",
        compact && "justify-center px-2"
      )}
    >
      <Link
        to="/"
        onClick={onNavigate}
        aria-label="Servers"
        className="flex items-center gap-2"
      >
        <MonitorLogo />
        {!compact ? (
          <p className="text-lg font-bold leading-tight text-foreground">
            {APP_NAME}
          </p>
        ) : null}
      </Link>
      <button
        type="button"
        aria-label="Close sidebar"
        onClick={onMobileClose}
        className="ml-auto flex size-8 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground/80 lg:hidden dark:hover:bg-muted dark:hover:text-foreground"
      >
        <X className="size-4" />
      </button>
    </div>
  )
})

const AppSidebarFooter = memo(function AppSidebarFooter({
  compact,
  isLoggingOut,
  onLogout,
}: {
  compact: boolean
  isLoggingOut: boolean
  onLogout: () => void
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-2 border-t border-sidebar-border p-4",
        compact && "items-center px-2"
      )}
    >
      <div
        className={cn(
          "flex w-full items-center gap-3",
          compact ? "justify-center" : "justify-between"
        )}
      >
        {!compact ? (
          <span className="text-sm font-medium text-muted-foreground">
            Theme
          </span>
        ) : null}
        <ThemeSwitcher />
      </div>
      {compact ? (
        <Button
          type="button"
          variant="default"
          size="icon-sm"
          aria-label="Sign out"
          disabled={isLoggingOut}
          onClick={onLogout}
          className="w-full"
        >
          {isLoggingOut ? <Spinner /> : <LogOut className="size-3.5" />}
        </Button>
      ) : (
        <Button
          type="button"
          variant="default"
          disabled={isLoggingOut}
          onClick={onLogout}
        >
          {isLoggingOut ? <Spinner /> : null}
          Sign out
        </Button>
      )}
    </div>
  )
})

export function AppSidebar({
  user,
  width,
  compact,
  collapsed,
  isResizing,
  mobileOpen,
  onToggleCollapsed,
  onResizeStart,
  onMobileClose,
  isLoggingOut,
  onLogout,
}: AppSidebarProps) {
  const handleNavigate = useCallback(() => {
    onMobileClose()
  }, [onMobileClose])

  return (
    <MobileSidebarDrawer
      mobileOpen={mobileOpen}
      onMobileClose={onMobileClose}
      width={width}
      isResizing={isResizing}
    >
      <AppSidebarHeader
        compact={compact}
        onNavigate={handleNavigate}
        onMobileClose={onMobileClose}
      />

      <nav className="relative flex min-h-0 flex-1 flex-col gap-1 overflow-hidden px-2">
        <button
          type="button"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          onClick={onToggleCollapsed}
          className="absolute -top-7 -right-3 z-10 hidden size-6 items-center justify-center rounded-full border border-border bg-card text-muted-foreground shadow-sm transition-colors hover:bg-muted hover:text-muted-foreground lg:flex dark:hover:bg-muted dark:hover:text-foreground"
        >
          <ChevronLeft
            className={cn(
              "size-3.5 transition-transform",
              collapsed && "rotate-180"
            )}
          />
        </button>
        <SidebarNav compact={compact} onNavigate={handleNavigate} />
        {user.role === "ADMIN" ? (
          <SidebarAdminSection
            compact={compact}
            onNavigate={handleNavigate}
          />
        ) : null}
        <SidebarFolderTree compact={compact} onNavigate={handleNavigate} />
      </nav>

      <SidebarResizeHandle onResizeStart={onResizeStart} />

      <AppSidebarFooter
        compact={compact}
        isLoggingOut={isLoggingOut}
        onLogout={onLogout}
      />
    </MobileSidebarDrawer>
  )
}
