import { useNavigate } from "@tanstack/react-router"
import {
  Gauge,
  Mail,
  Monitor,
  Moon,
  Search,
  Server,
  Settings,
  Sun,
  User as UserIcon,
} from "lucide-react"
import type { LucideIcon } from "lucide-react"
import { useEffect, useId, useMemo, useRef, useState } from "react"
import type { KeyboardEvent } from "react"

import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import { useUserServers } from "@/hooks/use-user-servers"
import type { ServerStatus } from "@/lib/api/user/servers"
import { useAuth } from "@/lib/auth"
import { useTheme } from "@/lib/theme"
import { cn } from "@/lib/utils"

const OPEN_EVENT = "monitor:open-command-palette"

/** Opens the palette from anywhere (e.g. a header button). */
export function openCommandPalette() {
  window.dispatchEvent(new Event(OPEN_EVENT))
}

type PaletteItem = {
  id: string
  group: "Servers" | "Pages" | "Appearance"
  label: string
  detail?: string
  icon: LucideIcon
  status?: ServerStatus
  keywords: string
  run: () => void
}

const STATUS_DOT: Record<ServerStatus, string> = {
  ONLINE: "bg-green-500",
  OFFLINE: "bg-red-500",
  PENDING: "bg-amber-500",
}

const GROUP_ORDER: PaletteItem["group"][] = ["Servers", "Pages", "Appearance"]

const MAX_RESULTS_PER_GROUP = 8

/** Every whitespace-separated term must appear somewhere in the haystack. */
function matches(item: PaletteItem, query: string): boolean {
  const terms = query.toLowerCase().split(/\s+/).filter(Boolean)
  if (terms.length === 0) {
    return true
  }

  const haystack = `${item.label} ${item.keywords}`.toLowerCase()
  return terms.every((term) => haystack.includes(term))
}

function isPaletteShortcut(event: globalThis.KeyboardEvent): boolean {
  return (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k"
}

function CommandPalette() {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const [activeIndex, setActiveIndex] = useState(0)
  const listRef = useRef<HTMLDivElement>(null)
  const listId = useId()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { setTheme } = useTheme()
  const { defaultRange } = useMetricDefaultRange()
  const { data: servers = [] } = useUserServers()

  useEffect(() => {
    function onKeyDown(event: globalThis.KeyboardEvent) {
      if (isPaletteShortcut(event)) {
        event.preventDefault()
        setOpen((current) => !current)
      }
    }

    function onOpenRequest() {
      setOpen(true)
    }

    window.addEventListener("keydown", onKeyDown)
    window.addEventListener(OPEN_EVENT, onOpenRequest)
    return () => {
      window.removeEventListener("keydown", onKeyDown)
      window.removeEventListener(OPEN_EVENT, onOpenRequest)
    }
  }, [])

  const isAdmin = user?.role === "ADMIN"

  const items = useMemo<PaletteItem[]>(() => {
    const go = (run: () => void) => () => {
      setOpen(false)
      run()
    }

    const serverItems: PaletteItem[] = servers.map((server) => ({
      id: `server-${server.serverId}`,
      group: "Servers",
      label: server.serverName,
      detail: server.folderName ?? undefined,
      icon: Server,
      status: server.status,
      keywords: `${server.status} ${server.folderName ?? ""}`,
      run: go(() => {
        void navigate({
          to: "/servers/$serverId",
          params: { serverId: String(server.serverId) },
          search: { range: defaultRange },
        })
      }),
    }))

    const pages: PaletteItem[] = [
      {
        id: "page-servers",
        group: "Pages",
        label: "All servers",
        icon: Server,
        keywords: "dashboard home fleet overview",
        run: go(() => void navigate({ to: "/" })),
      },
      {
        id: "page-invites",
        group: "Pages",
        label: "Invites",
        icon: Mail,
        keywords: "pending access share",
        run: go(() => void navigate({ to: "/invites" })),
      },
      {
        id: "page-account",
        group: "Pages",
        label: "Account settings",
        icon: UserIcon,
        keywords: "profile password sessions preferences",
        run: go(() => void navigate({ to: "/settings" })),
      },
    ]

    if (isAdmin) {
      pages.push(
        {
          id: "page-admin-settings",
          group: "Pages",
          label: "Admin settings",
          icon: Settings,
          keywords: "registration administration",
          run: go(() => void navigate({ to: "/admin/settings" })),
        },
        {
          id: "page-admin-metrics",
          group: "Pages",
          label: "Admin metrics",
          icon: Gauge,
          keywords: "administration instance usage",
          run: go(
            () =>
              void navigate({
                to: "/admin/metrics",
                search: { range: defaultRange },
              })
          ),
        }
      )
    }

    const appearance: PaletteItem[] = [
      {
        id: "theme-light",
        group: "Appearance",
        label: "Use light theme",
        icon: Sun,
        keywords: "theme appearance bright",
        run: go(() => setTheme("light")),
      },
      {
        id: "theme-dark",
        group: "Appearance",
        label: "Use dark theme",
        icon: Moon,
        keywords: "theme appearance night",
        run: go(() => setTheme("dark")),
      },
      {
        id: "theme-system",
        group: "Appearance",
        label: "Use system theme",
        icon: Monitor,
        keywords: "theme appearance auto",
        run: go(() => setTheme("system")),
      },
    ]

    return [...serverItems, ...pages, ...appearance]
  }, [servers, isAdmin, navigate, defaultRange, setTheme])

  const results = useMemo(() => {
    const filtered = items.filter((item) => matches(item, query))

    return GROUP_ORDER.flatMap((group) =>
      filtered
        .filter((item) => item.group === group)
        .slice(0, MAX_RESULTS_PER_GROUP)
    )
  }, [items, query])

  const safeActiveIndex = Math.min(activeIndex, Math.max(results.length - 1, 0))
  const activeItem = results.at(safeActiveIndex)

  useEffect(() => {
    if (!open) {
      return
    }

    listRef.current
      ?.querySelector<HTMLElement>('[aria-selected="true"]')
      ?.scrollIntoView({ block: "nearest" })
  }, [open, safeActiveIndex, results])

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (next) {
      setQuery("")
      setActiveIndex(0)
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "ArrowDown") {
      event.preventDefault()
      setActiveIndex((safeActiveIndex + 1) % Math.max(results.length, 1))
    } else if (event.key === "ArrowUp") {
      event.preventDefault()
      setActiveIndex(
        (safeActiveIndex - 1 + Math.max(results.length, 1)) %
          Math.max(results.length, 1)
      )
    } else if (event.key === "Enter" && activeItem) {
      event.preventDefault()
      activeItem.run()
    }
  }

  let previousGroup: PaletteItem["group"] | null = null

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        showCloseButton={false}
        className="top-[18%] translate-y-0 gap-0 overflow-hidden border border-border p-0 sm:max-w-xl"
        aria-describedby={undefined}
      >
        <DialogTitle className="sr-only">Command palette</DialogTitle>

        <div className="flex items-center gap-2 border-b border-border px-3">
          <Search aria-hidden className="size-4 shrink-0 text-muted-foreground" />
          <input
            autoFocus
            role="combobox"
            aria-expanded
            aria-controls={listId}
            aria-activedescendant={
              activeItem ? `${listId}-${activeItem.id}` : undefined
            }
            aria-label="Search servers and pages"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              setActiveIndex(0)
            }}
            onKeyDown={handleKeyDown}
            placeholder="Jump to a server or page…"
            className="h-12 w-full bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground"
          />
          <kbd className="hidden shrink-0 rounded-sm border border-border px-1.5 py-0.5 text-[11px] text-muted-foreground sm:block">
            Esc
          </kbd>
        </div>

        <div
          ref={listRef}
          id={listId}
          role="listbox"
          aria-label="Results"
          className="max-h-80 overflow-y-auto p-1.5"
        >
          {results.length === 0 ? (
            <p className="px-3 py-8 text-center text-sm text-muted-foreground">
              No results for “{query}”.
            </p>
          ) : (
            results.map((item, index) => {
              const showHeading = item.group !== previousGroup
              previousGroup = item.group
              const active = index === safeActiveIndex

              return (
                <div key={item.id} role="presentation">
                  {showHeading ? (
                    <p
                      role="presentation"
                      className="px-2 pt-2 pb-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase"
                    >
                      {item.group}
                    </p>
                  ) : null}
                  <div
                    id={`${listId}-${item.id}`}
                    role="option"
                    aria-selected={active}
                    onMouseMove={() => setActiveIndex(index)}
                    onClick={item.run}
                    className={cn(
                      "flex cursor-pointer items-center gap-3 rounded-sm px-2 py-2 text-sm",
                      active
                        ? "bg-primary/10 text-foreground"
                        : "text-muted-foreground"
                    )}
                  >
                    <item.icon
                      aria-hidden
                      className={cn("size-4 shrink-0", active && "text-brand")}
                    />
                    <span className="min-w-0 flex-1 truncate">{item.label}</span>
                    {item.detail ? (
                      <span className="shrink-0 truncate text-xs text-muted-foreground">
                        {item.detail}
                      </span>
                    ) : null}
                    {item.status ? (
                      <span className="flex shrink-0 items-center gap-1.5 text-xs">
                        <span
                          aria-hidden
                          className={cn(
                            "size-1.5 rounded-full",
                            STATUS_DOT[item.status]
                          )}
                        />
                        <span className="capitalize">
                          {item.status.toLowerCase()}
                        </span>
                      </span>
                    ) : null}
                  </div>
                </div>
              )
            })
          )}
        </div>

        <div className="flex items-center gap-4 border-t border-border px-3 py-2 text-[11px] text-muted-foreground">
          <span>
            <kbd className="font-sans">↑↓</kbd> navigate
          </span>
          <span>
            <kbd className="font-sans">↵</kbd> open
          </span>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export { CommandPalette }
