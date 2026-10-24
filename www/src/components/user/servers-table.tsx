import { Search } from "lucide-react"
import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"

import { CreateFolderDialog } from "@/components/user/folder-name-dialog"
import { CreateServerDialog } from "@/components/user/create-server-dialog"
import { FleetSummaryCards } from "@/components/user/fleet-summary-cards"
import { FirstServerPrompt } from "@/components/user/first-server-prompt"
import { ServersFilterBar } from "@/components/user/servers-filter-bar"
import {
  filterServerIdsBySearch,
  getServerTableColumns,
} from "@/components/user/server-table-columns"
import { ServersFolderTable } from "@/components/user/servers-folder-table"
import { QueryStatusShell } from "@/components/query-status-shell"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import {
  useServerFolderLayout,
  useServerIds,
} from "@/hooks/use-server-folder-layout"
import { useServersTableOrganize } from "@/hooks/use-servers-table-organize"
import { useUserServers } from "@/hooks/use-user-servers"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServerFoldersQueryOptions } from "@/lib/api/user/folders.queries"
import { hasPermission, ServerPermission } from "@/lib/api/user/permissions"
import { serversById } from "@/lib/api/user/servers.queries"
import {
  countByFleetFilter,
  serverMatchesFilter,
  sortServerIds,
} from "@/lib/servers/fleet-filter"
import type { FleetFilter, ServerSort } from "@/lib/servers/fleet-filter"

const EMPTY_FOLDERS: ServerFolderResponse[] = []

function ServersTable() {
  const { defaultRange } = useMetricDefaultRange()
  const [searchQuery, setSearchQuery] = useState("")
  const [editMode, setEditMode] = useState(false)
  const [fleetFilter, setFleetFilter] = useState<FleetFilter>("all")
  const [sortKey, setSortKey] = useState<ServerSort>("attention")

  const serverIds = useServerIds()
  const { byFolder, ungroupedIds } = useServerFolderLayout()
  const { data: servers = [], isPending, error } = useUserServers()
  const serversMap = useMemo(() => serversById(servers), [servers])
  const { data: folders = EMPTY_FOLDERS, isPending: foldersPending } = useQuery(
    userServerFoldersQueryOptions()
  )

  const {
    draggingServerId,
    draggingFolderId,
    activeDropTargetKey,
    canAcceptServerDrop,
    onServerDragStart,
    onServerDragEnd,
    onFolderDragStart,
    onFolderDragEnd,
    onDropTargetChange,
    onMoveServer,
    onReorderFolder,
  } = useServersTableOrganize({ editMode, folders, serversMap })

  const showActionsColumn = servers.some(
    (server) =>
      hasPermission(server.permissions, ServerPermission.RENAME_SERVER) ||
      hasPermission(server.permissions, ServerPermission.DELETE_SERVER)
  )
  const columns = useMemo(
    () => getServerTableColumns(showActionsColumn, defaultRange),
    [showActionsColumn, defaultRange]
  )

  // Reorganising only makes sense on the full, user-ordered list.
  const activeFilter: FleetFilter = editMode ? "all" : fleetFilter
  const activeSort: ServerSort = editMode ? "custom" : sortKey
  const fleetCounts = useMemo(() => countByFleetFilter(servers), [servers])

  const visibleIds = useMemo(
    () => (ids: number[]) =>
      sortServerIds(
        filterServerIdsBySearch(
          ids.filter((id) => serverMatchesFilter(serversMap[id], activeFilter)),
          searchQuery,
          serversMap
        ),
        serversMap,
        activeSort
      ),
    [activeFilter, activeSort, searchQuery, serversMap]
  )

  const filteredUngroupedIds = useMemo(
    () => visibleIds(ungroupedIds),
    [ungroupedIds, visibleIds]
  )

  const folderSections = useMemo(
    () =>
      folders.map((folder) => ({
        folder,
        serverIds: visibleIds(byFolder.get(folder.name) ?? []),
      })),
    [folders, byFolder, visibleIds]
  )

  const isFiltering = searchQuery.trim() !== "" || activeFilter !== "all"

  const showUngroupedSection =
    filteredUngroupedIds.length > 0 || (editMode && folders.length > 0)
  const visibleSectionCount =
    folderSections.filter((section) => section.serverIds.length > 0).length +
    (filteredUngroupedIds.length > 0 ? 1 : 0)
  const hasEmptyFolderSections =
    !isFiltering &&
    folderSections.some((section) => section.serverIds.length === 0)
  const hasEmptyUngroupedSection =
    !isFiltering &&
    editMode &&
    folders.length > 0 &&
    filteredUngroupedIds.length === 0

  const isLoading = (isPending || foldersPending) && !error
  const canOrganize = folders.length > 0 && serverIds.length > 0
  const canReorderFolders = folders.length > 1
  const showEditMode =
    !isLoading && (folders.length > 0 || serverIds.length > 0)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <div className="relative w-full min-w-0 sm:max-w-sm sm:flex-1">
          <Search
            aria-hidden
            className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            type="search"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search servers…"
            aria-label="Search servers"
            className="pl-9 text-base sm:text-sm"
          />
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2 sm:shrink-0">
          {showEditMode ? (
            <Button
              type="button"
              variant={editMode ? "highlighted" : "default"}
              size="sm"
              onClick={() => setEditMode((current) => !current)}
            >
              {editMode ? "Done" : "Edit"}
            </Button>
          ) : null}
          <CreateFolderDialog />
          <CreateServerDialog />
        </div>
      </div>

      <QueryStatusShell
        error={error}
        isPending={isLoading}
        loadingMessage="Loading servers…"
        fallbackMessage="Failed to load servers"
        fallbackTitle="Could not load servers"
        className="flex flex-col gap-6"
      >
        {servers.length > 0 ? (
          <FleetSummaryCards
            servers={servers}
            activeFilter={activeFilter}
            onFilterChange={setFleetFilter}
          />
        ) : null}

        {servers.length > 0 && !editMode ? (
          <ServersFilterBar
            counts={fleetCounts}
            filter={fleetFilter}
            onFilterChange={setFleetFilter}
            sort={sortKey}
            onSortChange={setSortKey}
          />
        ) : null}

        {editMode && (canOrganize || canReorderFolders) ? (
          <p className="text-xs text-muted-foreground">
            {canOrganize && canReorderFolders
              ? "Drag servers into folders, or drag folder headers to reorder."
              : canOrganize
                ? "Drag servers by the grip handle into a folder to organize them."
                : "Drag folder headers to reorder them."}
          </p>
        ) : null}

        {serverIds.length === 0 && folders.length === 0 ? (
          <FirstServerPrompt />
        ) : null}

        {isFiltering &&
        visibleSectionCount === 0 &&
        !hasEmptyFolderSections &&
        !hasEmptyUngroupedSection ? (
          <div className="flex flex-col items-start gap-2 rounded-lg border border-dashed border-border px-4 py-6">
            <p className="text-muted-foreground">
              No servers match your current filters.
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setSearchQuery("")
                setFleetFilter("all")
              }}
            >
              Clear filters
            </Button>
          </div>
        ) : null}

        {folderSections.some(
          ({ serverIds: sectionServerIds }) =>
            !isFiltering || sectionServerIds.length > 0
        ) ||
        filteredUngroupedIds.length > 0 ||
        folders.length > 0 ? (
          <div className="flex flex-col">
            {folderSections
              .filter(
                ({ serverIds: sectionServerIds }) =>
                  !isFiltering || sectionServerIds.length > 0
              )
              .map(({ folder, serverIds: sectionServerIds }) => (
                <ServersFolderTable
                  key={folder.id}
                  title={folder.name}
                  folderId={folder.id}
                  folderName={folder.name}
                  dropTargetKey={`folder:${folder.id}`}
                  isDropTarget={activeDropTargetKey === `folder:${folder.id}`}
                  canAcceptServerDrop={canAcceptServerDrop(folder.name)}
                  serverIds={sectionServerIds}
                  editMode={editMode}
                  columns={columns}
                  serversMap={serversMap}
                  draggingServerId={draggingServerId}
                  draggingFolderId={draggingFolderId}
                  onServerDragStart={onServerDragStart}
                  onServerDragEnd={onServerDragEnd}
                  onFolderDragStart={onFolderDragStart}
                  onFolderDragEnd={onFolderDragEnd}
                  onDropTargetChange={onDropTargetChange}
                  onMoveServer={onMoveServer}
                  onReorderFolder={onReorderFolder}
                />
              ))}
            {showUngroupedSection ? (
              <ServersFolderTable
                title="Ungrouped"
                folderName={null}
                dropTargetKey="ungrouped"
                isDropTarget={activeDropTargetKey === "ungrouped"}
                canAcceptServerDrop={canAcceptServerDrop(null)}
                serverIds={filteredUngroupedIds}
                editMode={editMode}
                columns={columns}
                serversMap={serversMap}
                draggingServerId={draggingServerId}
                draggingFolderId={draggingFolderId}
                onServerDragStart={onServerDragStart}
                onServerDragEnd={onServerDragEnd}
                onFolderDragStart={onFolderDragStart}
                onFolderDragEnd={onFolderDragEnd}
                onDropTargetChange={onDropTargetChange}
                onMoveServer={onMoveServer}
                onReorderFolder={onReorderFolder}
              />
            ) : null}
          </div>
        ) : null}
      </QueryStatusShell>
    </div>
  )
}

export { ServersTable }
