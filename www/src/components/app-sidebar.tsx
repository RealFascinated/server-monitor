import { Link } from "@tanstack/react-router"
import {
  ChevronLeft,
  ChevronRight,
  Gauge,
  LogOut,
  Mail,
  Search,
  Server,
  Settings,
  User as UserIcon,
  X,
} from "lucide-react"
import type { CSSProperties, PointerEvent as ReactPointerEvent } from "react"
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react"
import { useQuery, useQueryClient } from "@tanstack/react-query"

import { CpuPercent, MemoryPercent } from "@/components/server/usage-percent"
import { CountBadge } from "@/components/count-badge"
import { MonitorLogo } from "@/components/monitor-logo"
import { CollapsiblePanel } from "@/components/collapsible-panel"
import { SimpleTooltip } from "@/components/simple-tooltip"
import { Spinner } from "@/components/spinner"
import { ThemeSwitcher } from "@/components/theme-switcher"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { filterServerIdsBySearch } from "@/components/user/server-table-columns"
import { useSidebarColumnVisibility } from "@/hooks/use-sidebar-column-visibility"
import { useSidebarDetailedMode } from "@/hooks/use-sidebar-detailed-mode"
import {
  useServerFolderLayout,
  useServerIds,
} from "@/hooks/use-server-folder-layout"
import { useUserInvites } from "@/hooks/use-user-invites"
import { useUserServers } from "@/hooks/use-user-servers"
import { userServerFoldersQueryOptions } from "@/lib/api/user/folders.queries"
import { userServerMetricsQueryOptions } from "@/lib/api/user/metrics.queries"
import {
  resolveUserServerFromCache,
  serversById,
} from "@/lib/api/user/servers.queries"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import type { ServerResponse, ServerStatus } from "@/lib/api/user/servers"
import type { User } from "@/lib/auth/types"
import {
  defaultMetricRangeSearch,
  getStoredDefaultMetricRange,
} from "@/lib/metrics/default-range"
import { SERVER_STATUS_TOOLTIPS } from "@/lib/tooltips/copy"
import { cn } from "@/lib/utils"

const MOBILE_SIDEBAR_WIDTH = 280

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

const statusDotStyles: Record<ServerStatus, string> = {
  ONLINE: "bg-green-500",
  OFFLINE: "bg-red-500",
  PENDING: "bg-amber-500",
}


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
        "[&.active]:bg-neutral-200 [&.active]:text-black dark:[&.active]:bg-monitor-gray-200 dark:[&.active]:text-warning",
        compact && "justify-center px-0",
        compact && "cursor-pointer"
      )}
    >
      <span className="relative shrink-0">
        <Icon className="size-4" />
        {compact ? <CountBadge count={badgeCount} variant="accent" compact /> : null}
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

function SidebarAdminLink({
  compact,
  onNavigate,
  to,
  search,
  icon: Icon,
  label,
}: {
  compact: boolean
  onNavigate?: () => void
  to: "/admin/metrics" | "/admin/settings"
  search?: ReturnType<typeof defaultMetricRangeSearch>
  icon: typeof Gauge
  label: string
}) {
  const link = (
    <Link
      to={to}
      search={search}
      onClick={onNavigate}
      className={cn(
        "flex min-h-7 w-full items-center gap-3 rounded-sm px-2 py-1 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted",
        "[&.active]:bg-neutral-200 [&.active]:text-black dark:[&.active]:bg-monitor-gray-200 dark:[&.active]:text-warning",
        compact && "justify-center px-0",
        compact && "cursor-pointer"
      )}
    >
      <Icon className="size-4 shrink-0" />
      {!compact ? <span className="truncate">{label}</span> : null}
    </Link>
  )

  if (compact) {
    return <SimpleTooltip content={label}>{link}</SimpleTooltip>
  }

  return link
}

function SidebarAdminSection({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  return (
    <div className="flex shrink-0 flex-col">
      {!compact ? (
        <p className="mt-3 mb-1 px-2 text-xs font-medium tracking-wide text-neutral-400 uppercase">
          Admin
        </p>
      ) : (
        <div className="my-2 shrink-0 border-t border-sidebar-border" />
      )}

      <SidebarAdminLink
        compact={compact}
        onNavigate={onNavigate}
        to="/admin/settings"
        icon={Settings}
        label="Settings"
      />
      <SidebarAdminLink
        compact={compact}
        onNavigate={onNavigate}
        to="/admin/metrics"
        search={defaultMetricRangeSearch()}
        icon={Gauge}
        label="Metrics"
      />
    </div>
  )
}

const UNGROUPED_SIDEBAR_KEY = "Ungrouped"
const EMPTY_FOLDERS: ServerFolderResponse[] = []

const SidebarServerItem = memo(
  function SidebarServerItem({
    server,
    compact,
    detailed,
    onNavigate,
    nested = false,
  }: {
    server: ServerResponse
    compact: boolean
    detailed: boolean
    onNavigate?: () => void
    nested?: boolean
  }) {
    const { visibility } = useSidebarColumnVisibility()
    const queryClient = useQueryClient()

    const prefetchServer = useCallback(() => {
      resolveUserServerFromCache(queryClient, server.serverId)
      void queryClient.prefetchQuery(
        userServerMetricsQueryOptions(server.serverId, {
          kind: "preset",
          range: getStoredDefaultMetricRange(),
        })
      )
    }, [queryClient, server.serverId])

  const serverTooltip = `${server.serverName} — ${SERVER_STATUS_TOOLTIPS[server.status]}`

  const link = (
    <Link
      to="/servers/$serverId"
      params={{ serverId: String(server.serverId) }}
      search={defaultMetricRangeSearch()}
      onClick={onNavigate}
      onMouseEnter={prefetchServer}
      onFocus={prefetchServer}
      className={cn(
        "flex w-full shrink-0 rounded-sm text-sm font-medium text-muted-foreground transition-colors hover:bg-muted",
        "[&.active]:bg-neutral-200 [&.active]:text-black dark:[&.active]:bg-monitor-gray-200 dark:[&.active]:text-warning",
        nested && !compact && "pl-4",
        compact
          ? "min-h-7 cursor-pointer items-center justify-center gap-3 px-0 py-1"
          : detailed
            ? "items-center gap-2 px-2 py-1"
            : "min-h-7 items-center gap-3 px-2 py-1"
      )}
    >
      <span className="relative shrink-0">
        <Server className="size-4" />
        <span
          className={cn(
            "absolute -right-0.5 -bottom-0.5 size-1.5 rounded-full ring-2 ring-white dark:ring-base",
            statusDotStyles[server.status]
          )}
        />
      </span>
      {!compact ? (
        <span
          className={cn(
            "flex min-w-0 flex-1 flex-col",
            detailed ? "gap-0 leading-tight" : "gap-0.5"
          )}
        >
          <span className="truncate leading-tight">{server.serverName}</span>
          {detailed && (visibility.cpu || visibility.ram) ? (
            <span className="truncate text-[11px] leading-tight text-neutral-400">
              {visibility.cpu ? (
                <>
                  CPU{" "}
                  <CpuPercent
                    cpu={server.cpu}
                    status={server.status}
                    className="font-medium"
                  />
                </>
              ) : null}
              {visibility.cpu && visibility.ram ? " · " : null}
              {visibility.ram ? (
                <>
                  RAM{" "}
                  <MemoryPercent
                    usage={server.memory?.usage ?? null}
                    max={server.memory?.max ?? null}
                    status={server.status}
                    className="font-medium"
                  />
                </>
              ) : null}
            </span>
          ) : null}
        </span>
      ) : null}
    </Link>
  )

  if (compact) {
    return <SimpleTooltip content={serverTooltip}>{link}</SimpleTooltip>
  }

  return link
  },
  (prev, next) =>
    prev.server === next.server &&
    prev.compact === next.compact &&
    prev.detailed === next.detailed &&
    prev.nested === next.nested
)

const SidebarFolderGroup = memo(function SidebarFolderGroup({
  folderName,
  serverIds,
  getServer,
  compact,
  detailed,
  expanded,
  onToggle,
  onNavigate,
}: {
  folderName: string
  serverIds: number[]
  getServer: (serverId: number) => ServerResponse
  compact: boolean
  detailed: boolean
  expanded: boolean
  onToggle: () => void
  onNavigate?: () => void
}) {
  if (compact) {
    return (
      <>
        {serverIds.map((serverId) => (
          <SidebarServerItem
            key={serverId}
            server={getServer(serverId)}
            compact={compact}
            detailed={detailed}
            onNavigate={onNavigate}
          />
        ))}
      </>
    )
  }

  return (
    <div className="flex flex-col">
      <div className="flex items-center gap-0.5 pr-1">
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={expanded}
          className="flex min-w-0 flex-1 items-center gap-1 rounded-sm px-2 py-1 text-left text-xs leading-snug font-medium text-muted-foreground transition-colors hover:bg-muted/80 hover:text-foreground"
        >
          <ChevronRight
            aria-hidden
            className={cn(
              "size-3 shrink-0 opacity-60 transition-transform duration-150",
              expanded && "rotate-90"
            )}
          />
          <span className="truncate">{folderName}</span>
          <CountBadge count={serverIds.length} className="ml-auto" />
        </button>
      </div>

      <CollapsiblePanel open={expanded} className="flex flex-col gap-px pb-0.5">
        {serverIds.map((serverId) => (
          <SidebarServerItem
            key={serverId}
            server={getServer(serverId)}
            compact={compact}
            detailed={detailed}
            onNavigate={onNavigate}
            nested
          />
        ))}
      </CollapsiblePanel>
    </div>
  )
})

function SidebarServerList({
  compact,
  onNavigate,
}: {
  compact: boolean
  onNavigate?: () => void
}) {
  const [searchQuery, setSearchQuery] = useState("")
  const serverIds = useServerIds()
  const { byFolder, ungroupedIds } = useServerFolderLayout()
  const { data: folders = EMPTY_FOLDERS } = useQuery(
    userServerFoldersQueryOptions()
  )
  const { data: servers = [] } = useUserServers()
  const serversMap = useMemo(() => serversById(servers), [servers])
  const getServer = useCallback(
    (serverId: number) => serversMap[serverId],
    [serversMap]
  )
  const { detailed } = useSidebarDetailedMode()
  const search = searchQuery.trim()

  const filteredServerIds = useMemo(
    () => filterServerIdsBySearch(serverIds, searchQuery, serversMap),
    [serverIds, searchQuery, serversMap]
  )

  const filteredUngroupedIds = useMemo(
    () => filterServerIdsBySearch(ungroupedIds, searchQuery, serversMap),
    [ungroupedIds, searchQuery, serversMap]
  )

  const filteredFolders = useMemo(
    () =>
      folders.map((folder) => ({
        folder,
        serverIds: filterServerIdsBySearch(
          byFolder.get(folder.name) ?? [],
          searchQuery,
          serversMap
        ),
      })),
    [folders, byFolder, searchQuery, serversMap]
  )

  const folderNamesKey = [
    ...folders.map((folder) => folder.name),
    UNGROUPED_SIDEBAR_KEY,
  ].join("\0")
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(() => {
    const initial = new Set(folders.map((folder) => folder.name))
    initial.add(UNGROUPED_SIDEBAR_KEY)
    return initial
  })
  const prevFolderNamesKeyRef = useRef(folderNamesKey)

  useEffect(() => {
    if (prevFolderNamesKeyRef.current === folderNamesKey) {
      return
    }

    const prevNames = new Set(prevFolderNamesKeyRef.current.split("\0"))
    const newNames = folderNamesKey
      .split("\0")
      .filter((name) => !prevNames.has(name))

    prevFolderNamesKeyRef.current = folderNamesKey

    if (newNames.length === 0) {
      return
    }

    setExpandedFolders((current) => {
      const next = new Set(current)
      for (const name of newNames) {
        next.add(name)
      }
      return next
    })
  }, [folderNamesKey])

  useEffect(() => {
    if (!search) {
      return
    }

    setExpandedFolders((current) => {
      const next = new Set(current)
      let changed = false

      for (const { folder, serverIds: ids } of filteredFolders) {
        if (ids.length > 0 && !next.has(folder.name)) {
          next.add(folder.name)
          changed = true
        }
      }

      if (filteredUngroupedIds.length > 0 && !next.has(UNGROUPED_SIDEBAR_KEY)) {
        next.add(UNGROUPED_SIDEBAR_KEY)
        changed = true
      }

      return changed ? next : current
    })
  }, [search, filteredFolders, filteredUngroupedIds])

  function toggleFolder(folderName: string) {
    setExpandedFolders((current) => {
      const next = new Set(current)
      if (next.has(folderName)) {
        next.delete(folderName)
      } else {
        next.add(folderName)
      }
      return next
    })
  }

  const hasFolders = folders.length > 0
  const showGroupedSidebar = hasFolders && !compact
  const visibleFolders =
    search === ""
      ? filteredFolders
      : filteredFolders.filter(({ serverIds: ids }) => ids.length > 0)
  const showUngrouped = filteredUngroupedIds.length > 0
  const hasSearchResults =
    search === "" ||
    (showGroupedSidebar
      ? visibleFolders.some(({ serverIds: ids }) => ids.length > 0) ||
        filteredUngroupedIds.length > 0
      : filteredServerIds.length > 0)

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {!compact ? (
        <>
          <div className="mt-3 mb-0 shrink-0 px-2">
            <p className="text-xs font-medium tracking-wide text-neutral-400 uppercase">
              Servers
            </p>
          </div>
          <div className="relative my-2 shrink-0 px-2">
            <Search
              aria-hidden
              className="pointer-events-none absolute top-1/2 left-4 size-3.5 -translate-y-1/2 text-neutral-400"
            />
            <Input
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search servers…"
              aria-label="Search servers"
              className="h-7 pl-8 text-xs"
            />
          </div>
        </>
      ) : (
        <div className="my-2 shrink-0 border-t border-sidebar-border" />
      )}
      <div className="flex min-h-0 flex-1 flex-col gap-0.5 overflow-y-auto">
        {!hasSearchResults ? (
          <p className="px-2 py-1 text-xs text-muted-foreground">
            No servers match your search.
          </p>
        ) : null}
        {showGroupedSidebar
          ? visibleFolders.map(({ folder, serverIds: ids }) => (
              <SidebarFolderGroup
                key={folder.id}
                folderName={folder.name}
                serverIds={ids}
                getServer={getServer}
                compact={compact}
                detailed={detailed}
                expanded={expandedFolders.has(folder.name)}
                onToggle={() => toggleFolder(folder.name)}
                onNavigate={onNavigate}
              />
            ))
          : null}
        {showGroupedSidebar && showUngrouped ? (
          <SidebarFolderGroup
            folderName={UNGROUPED_SIDEBAR_KEY}
            serverIds={filteredUngroupedIds}
            getServer={getServer}
            compact={compact}
            detailed={detailed}
            expanded={expandedFolders.has(UNGROUPED_SIDEBAR_KEY)}
            onToggle={() => toggleFolder(UNGROUPED_SIDEBAR_KEY)}
            onNavigate={onNavigate}
          />
        ) : null}
        {(compact || !hasFolders ? filteredServerIds : []).map((serverId) => (
          <SidebarServerItem
            key={serverId}
            server={getServer(serverId)}
            compact={compact}
            detailed={detailed}
            onNavigate={onNavigate}
          />
        ))}
      </div>
    </div>
  )
}

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
  const { data: invites = [] } = useUserInvites()
  const pendingInviteCount = invites.length

  function handleNavigate() {
    onMobileClose()
  }

  return (
    <>
      <button
        type="button"
        aria-label="Close sidebar"
        aria-hidden={!mobileOpen}
        tabIndex={mobileOpen ? 0 : -1}
        onClick={onMobileClose}
        className={cn(
          "fixed inset-0 z-40 bg-black/50 transition-opacity duration-150 ease-out lg:hidden",
          mobileOpen
            ? "pointer-events-auto opacity-100"
            : "pointer-events-none opacity-0"
        )}
      />

      <aside
        style={
          {
            "--sidebar-inline-width": `${width}px`,
            "--mobile-sidebar-width": `${MOBILE_SIDEBAR_WIDTH}px`,
          } as CSSProperties
        }
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-[min(var(--mobile-sidebar-width),85vw)] flex-col border-r border-sidebar-border bg-sidebar lg:w-[length:var(--sidebar-inline-width)]",
          mobileOpen ? "translate-x-0" : "-translate-x-full",
          "lg:translate-x-0",
          !mobileOpen && "pointer-events-none lg:pointer-events-auto",
          "transition-transform duration-150 ease-out lg:duration-200 lg:ease-in-out",
          !isResizing && "lg:transition-[width,transform]"
        )}
      >
        <div
          className={cn(
            "flex items-center gap-2 p-4",
            compact && "justify-center px-2"
          )}
        >
          <Link
            to="/"
            onClick={handleNavigate}
            aria-label="Servers"
            className="flex items-center gap-2"
          >
            <MonitorLogo />
            {!compact ? (
              <p className="text-2xl font-bold tracking-wide text-black dark:text-white">
                Monitor
              </p>
            ) : null}
          </Link>
          <button
            type="button"
            aria-label="Close sidebar"
            onClick={onMobileClose}
            className="ml-auto flex size-8 items-center justify-center rounded-sm text-neutral-500 transition-colors hover:bg-neutral-200 hover:text-neutral-700 lg:hidden dark:hover:bg-monitor-gray-200 dark:hover:text-white"
          >
            <X className="size-4" />
          </button>
        </div>

        <nav className="relative flex min-h-0 flex-1 flex-col gap-1 overflow-hidden px-2">
          <button
            type="button"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            onClick={onToggleCollapsed}
            className="absolute -top-7 -right-3 z-10 hidden size-6 items-center justify-center rounded-full border border-neutral-200 bg-white text-neutral-400 shadow-sm transition-colors hover:bg-neutral-100 hover:text-neutral-600 lg:flex dark:border-monitor-gray-300 dark:bg-monitor-gray-100 dark:text-neutral-400 dark:hover:bg-monitor-gray-200 dark:hover:text-white"
          >
            <ChevronLeft
              className={cn(
                "size-3.5 transition-transform",
                collapsed && "rotate-180"
              )}
            />
          </button>
          {navItems.map(({ to, label, icon, exact }) => (
            <SidebarNavLink
              key={to}
              compact={compact}
              onNavigate={handleNavigate}
              to={to}
              icon={icon}
              label={label}
              exact={exact}
              badgeCount={to === "/invites" ? pendingInviteCount : 0}
            />
          ))}
          {user.role === "ADMIN" ? (
            <SidebarAdminSection
              compact={compact}
              onNavigate={handleNavigate}
            />
          ) : null}
          <SidebarServerList compact={compact} onNavigate={handleNavigate} />
        </nav>

        <div
          role="separator"
          aria-orientation="vertical"
          aria-label="Resize sidebar"
          onPointerDown={onResizeStart}
          className="absolute top-0 right-0 z-20 hidden h-full w-1 cursor-col-resize touch-none hover:bg-neutral-300/80 active:bg-neutral-400/80 lg:block dark:hover:bg-monitor-gray-300/80 dark:active:bg-monitor-gray-400/80"
        />

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
      </aside>
    </>
  )
}
