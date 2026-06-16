import { ChevronRight } from "lucide-react"
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react"
import { useQuery } from "@tanstack/react-query"

import { CountBadge } from "@/components/count-badge"
import { CollapsiblePanel } from "@/components/collapsible-panel"
import { SidebarServerItem } from "@/components/sidebar/sidebar-server-item"
import { filterServerIdsBySearch } from "@/components/user/server-table-columns"
import {
  useServerFolderLayout,
  useServerIds,
} from "@/hooks/use-server-folder-layout"
import { useSidebarDetailedMode } from "@/hooks/use-sidebar-detailed-mode"
import { useUserServers } from "@/hooks/use-user-servers"
import { userServerFoldersQueryOptions } from "@/lib/api/user/folders.queries"
import { serversById } from "@/lib/api/user/servers.queries"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import type { ServerResponse } from "@/lib/api/user/servers"
import { cn } from "@/lib/utils"

export const UNGROUPED_SIDEBAR_KEY = "Ungrouped"
const EMPTY_FOLDERS: ServerFolderResponse[] = []

const SidebarFolderGroup = memo(function SidebarFolderGroup({
  folderName,
  serverIds,
  getServer,
  compact,
  detailed,
  expanded,
  onToggleFolder,
  onNavigate,
}: {
  folderName: string
  serverIds: number[]
  getServer: (serverId: number) => ServerResponse
  compact: boolean
  detailed: boolean
  expanded: boolean
  onToggleFolder: (folderName: string) => void
  onNavigate?: () => void
}) {
  const handleToggle = useCallback(() => {
    onToggleFolder(folderName)
  }, [folderName, onToggleFolder])

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
          onClick={handleToggle}
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

type SidebarFolderListProps = {
  compact: boolean
  searchQuery: string
  onNavigate?: () => void
}

export const SidebarFolderList = memo(function SidebarFolderList({
  compact,
  searchQuery,
  onNavigate,
}: SidebarFolderListProps) {
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

  const toggleFolder = useCallback((folderName: string) => {
    setExpandedFolders((current) => {
      const next = new Set(current)
      if (next.has(folderName)) {
        next.delete(folderName)
      } else {
        next.add(folderName)
      }
      return next
    })
  }, [])

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
              onToggleFolder={toggleFolder}
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
          onToggleFolder={toggleFolder}
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
  )
})
