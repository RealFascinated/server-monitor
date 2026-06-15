import {
  Link,
  Outlet,
  createFileRoute,
  useNavigate,
  useRouterState,
} from "@tanstack/react-router"
import { Menu } from "lucide-react"
import { useQueryClient } from "@tanstack/react-query"
import { useEffect, useState } from "react"
import type { CSSProperties } from "react"

import { AppSidebar } from "@/components/app-sidebar"
import { LoadingState } from "@/components/loading-state"
import { MonitorLogo } from "@/components/monitor-logo"
import { NotFoundView } from "@/components/not-found-view"
import { useSidebarWidth } from "@/hooks/use-sidebar-width"
import { Button } from "@/components/ui/button"
import { userInvitesQueryOptions } from "@/lib/api/user/invites.queries"
import { userServersQueryOptions } from "@/lib/api/user/servers.queries"
import { logout, useAuth } from "@/lib/auth"
import { cn } from "@/lib/utils"

export const Route = createFileRoute("/_authenticated")({
  component: AuthenticatedLayout,
  notFoundComponent: () => <NotFoundView embedded />,
})

function AuthenticatedLayout() {
  const { user, isLoading, setUser } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  })
  const {
    width: sidebarWidth,
    compact: sidebarCompact,
    collapsed: sidebarCollapsed,
    isResizing: sidebarIsResizing,
    toggleCollapsed: toggleSidebarCollapsed,
    startResize: startSidebarResize,
  } = useSidebarWidth()

  useEffect(() => {
    if (!isLoading && !user) {
      void navigate({ to: "/login" })
    }
  }, [isLoading, user, navigate])

  useEffect(() => {
    if (!user) {
      return
    }
    void queryClient.prefetchQuery(userServersQueryOptions())
    void queryClient.prefetchQuery(userInvitesQueryOptions())
  }, [user, queryClient])

  useEffect(() => {
    setMobileSidebarOpen(false)
  }, [pathname])

  useEffect(() => {
    if (!mobileSidebarOpen) {
      return
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [mobileSidebarOpen])

  if (isLoading || !user) {
    return <LoadingState message="Checking session…" centered />
  }

  async function handleLogout() {
    setIsLoggingOut(true)
    try {
      await logout()
      queryClient.clear()
      setUser(null)
      await navigate({ to: "/login" })
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="bg-background">
      <AppSidebar
        user={user}
        width={sidebarWidth}
        compact={sidebarCompact && !mobileSidebarOpen}
        collapsed={sidebarCollapsed}
        isResizing={sidebarIsResizing}
        mobileOpen={mobileSidebarOpen}
        onToggleCollapsed={toggleSidebarCollapsed}
        onResizeStart={startSidebarResize}
        onMobileClose={() => setMobileSidebarOpen(false)}
        isLoggingOut={isLoggingOut}
        onLogout={handleLogout}
      />

      <div
        style={{ "--sidebar-width": `${sidebarWidth}px` } as CSSProperties}
        className={cn(
          "lg:pl-(--sidebar-width)",
          !sidebarIsResizing && "transition-[padding] duration-200"
        )}
      >
        <header className="sticky top-0 z-40 flex h-14 shrink-0 items-center gap-3 border-b border-sidebar-border bg-background/95 px-4 backdrop-blur-sm lg:hidden">
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label="Open sidebar"
            aria-expanded={mobileSidebarOpen}
            onClick={() => setMobileSidebarOpen(true)}
          >
            <Menu className="size-4" />
          </Button>
          <Link to="/" aria-label="Servers" className="flex items-center gap-3">
            <MonitorLogo />
            <p className="text-lg font-bold dark:text-white">Monitor</p>
          </Link>
        </header>

        <main className="p-4 sm:px-6 lg:px-8 lg:py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
