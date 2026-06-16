import { Search } from "lucide-react"
import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"

import { CreateFolderDialog } from "@/components/user/folder-name-dialog"
import { CreateServerDialog } from "@/components/user/create-server-dialog"
import { FleetSummaryCards } from "@/components/user/fleet-summary-cards"
import {
  filterServerIdsBySearch,
  getServerTableColumns,
} from "@/components/user/server-table-columns"
import { ServersFolderTable } from "@/components/user/servers-folder-table"
import { AsyncContent } from "@/components/animated-content"
import { Callout } from "@/components/callout"
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

const EMPTY_FOLDERS: ServerFolderResponse[] = []

function ServersTable() {
  const { defaultRange } = useMetricDefaultRange()
  const [searchQuery, setSearchQuery] = useState("")
  const [editMode, setEditMode] = useState(false)

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

  const filteredUngroupedIds = useMemo(
    () => filterServerIdsBySearch(ungroupedIds, searchQuery, serversMap),
    [ungroupedIds, searchQuery, serversMap]
  )

  const folderSections = useMemo(
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

  const showUngroupedSection =
    filteredUngroupedIds.length > 0 || (editMode && folders.length > 0)
  const visibleSectionCount =
    folderSections.filter((section) => section.serverIds.length > 0).length +
    (filteredUngroupedIds.length > 0 ? 1 : 0)
  const hasEmptyFolderSections =
    searchQuery.trim() === "" &&
    folderSections.some((section) => section.serverIds.length === 0)
  const hasEmptyUngroupedSection =
    searchQuery.trim() === "" &&
    editMode &&
    folders.length > 0 &&
    filteredUngroupedIds.length === 0

  const errorMessage = error instanceof Error ? error.message : null
  const isLoading = (isPending || foldersPending) && !errorMessage
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

      {errorMessage ? (
        <Callout type="danger" title="Could not load servers">
          {errorMessage}
        </Callout>
      ) : null}

      <AsyncContent loading={isLoading} loadingMessage="Loading servers…">
        <div className="flex flex-col gap-6">
          {servers.length > 0 ? <FleetSummaryCards servers={servers} /> : null}

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
            <p className="text-muted-foreground">No servers registered yet.</p>
          ) : null}

          {searchQuery.trim() !== "" &&
          visibleSectionCount === 0 &&
          !hasEmptyFolderSections &&
          !hasEmptyUngroupedSection ? (
            <p className="text-muted-foreground">No servers match your search.</p>
          ) : null}

          {folderSections.some(
            ({ serverIds: sectionServerIds }) =>
              searchQuery.trim() === "" || sectionServerIds.length > 0
          ) ||
          filteredUngroupedIds.length > 0 ||
          folders.length > 0 ? (
            <div className="flex flex-col">
              {folderSections
                .filter(
                  ({ serverIds: sectionServerIds }) =>
                    searchQuery.trim() === "" || sectionServerIds.length > 0
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
        </div>
      </AsyncContent>
    </div>
  )
}

export { ServersTable }
