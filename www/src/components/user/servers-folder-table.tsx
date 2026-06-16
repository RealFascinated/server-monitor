import {
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import type { ColumnDef, SortingState } from "@tanstack/react-table"
import { GripVertical, Plus } from "lucide-react"
import type { DragEvent } from "react"
import { memo, useMemo, useState } from "react"

import { CreateServerDialog } from "@/components/user/create-server-dialog"
import { CountBadge } from "@/components/count-badge"
import { DeleteFolderButton } from "@/components/user/delete-folder-button"
import { RenameFolderDialog } from "@/components/user/folder-name-dialog"
import type { ServerTableRow } from "@/components/user/server-table-columns"
import type { ServerResponse } from "@/lib/api/user/servers"
import { ServerCard } from "@/components/user/server-card"
import { ServerTableDataRow } from "@/components/user/server-table-row"
import { DataTable } from "@/components/ui/data-table"
import { beginFolderDrag, readDraggedServerId } from "@/lib/servers/drag"
import { cn } from "@/lib/utils"

type ServersFolderTableProps = {
  editMode: boolean
  title: string
  folderId?: number
  folderName: string | null
  dropTargetKey: string
  isDropTarget: boolean
  canAcceptServerDrop: boolean
  draggingServerId: number | null
  draggingFolderId: number | null
  serverIds: number[]
  columns: ColumnDef<ServerTableRow>[]
  onServerDragStart: (rowId: string) => void
  onServerDragEnd: () => void
  onFolderDragStart: (folderId: number) => void
  onFolderDragEnd: () => void
  onDropTargetChange: (dropTargetKey: string | null) => void
  onMoveServer: (serverId: number, folderName: string | null) => void
  onReorderFolder: (draggedFolderId: number, targetFolderId: number) => void
  serversMap: Record<number, ServerResponse>
}

function ServersFolderTableInner({
  editMode,
  title,
  folderId,
  folderName,
  dropTargetKey,
  isDropTarget,
  canAcceptServerDrop,
  draggingServerId,
  draggingFolderId,
  serverIds,
  columns,
  onServerDragStart,
  onServerDragEnd,
  onFolderDragStart,
  onFolderDragEnd,
  onDropTargetChange,
  onMoveServer,
  onReorderFolder,
  serversMap,
}: ServersFolderTableProps) {
  const isFolderDropTarget =
    editMode &&
    draggingFolderId != null &&
    folderId != null &&
    draggingFolderId !== folderId &&
    isDropTarget
  const isServerDropTarget = editMode && canAcceptServerDrop && isDropTarget
  const showDropTarget = isFolderDropTarget || isServerDropTarget
  const isDraggingFolder =
    editMode && folderId != null && draggingFolderId === folderId
  const [sorting, setSorting] = useState<SortingState>([])

  const tableData = useMemo(
    () =>
      serverIds.map((serverId) => ({
        serverId,
        server: serversMap[serverId],
      })),
    [serverIds, serversMap]
  )

  const table = useReactTable({
    data: tableData,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getRowId: (row) => String(row.serverId),
    state: { sorting },
    onSortingChange: setSorting,
  })

  function handleDragOver(event: DragEvent<HTMLElement>) {
    if (!editMode) {
      return
    }

    if (draggingFolderId != null) {
      if (folderId == null || draggingFolderId === folderId) {
        return
      }

      event.preventDefault()
      event.dataTransfer.dropEffect = "move"
      if (!isDropTarget) {
        onDropTargetChange(dropTargetKey)
      }
      return
    }

    if (draggingServerId == null) {
      return
    }

    if (!canAcceptServerDrop) {
      return
    }

    event.preventDefault()
    event.dataTransfer.dropEffect = "move"
    if (!isDropTarget) {
      onDropTargetChange(dropTargetKey)
    }
  }

  function handleDragLeave(event: DragEvent<HTMLElement>) {
    if (!editMode) {
      return
    }

    const related = event.relatedTarget
    if (related instanceof Node && event.currentTarget.contains(related)) {
      return
    }

    if (isDropTarget) {
      onDropTargetChange(null)
    }
  }

  function handleDrop(event: DragEvent<HTMLElement>) {
    if (!editMode) {
      return
    }

    event.preventDefault()
    onDropTargetChange(null)

    if (
      draggingFolderId != null &&
      folderId != null &&
      draggingFolderId !== folderId
    ) {
      onReorderFolder(draggingFolderId, folderId)
      return
    }

    const serverId = readDraggedServerId(event.dataTransfer) ?? draggingServerId
    if (serverId == null || !canAcceptServerDrop) {
      return
    }

    onMoveServer(serverId, folderName)
  }

  const dragHandlers = editMode
    ? {
        onDragOver: handleDragOver,
        onDragLeave: handleDragLeave,
        onDrop: handleDrop,
      }
    : undefined

  const rowDrag = editMode
    ? {
        draggingRowId:
          draggingServerId != null ? String(draggingServerId) : null,
        getServerId: (tableRow: { original: ServerTableRow }) =>
          tableRow.original.serverId,
        getServerLabel: (tableRow: { original: ServerTableRow }) =>
          tableRow.original.server.serverName,
        onDragStart: onServerDragStart,
        onDragEnd: onServerDragEnd,
      }
    : undefined

  return (
    <section
      className={cn(
        "flex flex-col gap-2 rounded-sm",
        showDropTarget &&
          "bg-monitor/5 ring-2 ring-monitor ring-inset dark:bg-warning/5 dark:ring-warning",
        isDraggingFolder && "opacity-40"
      )}
      {...dragHandlers}
    >
      <div className="flex items-center gap-1 px-1">
        {editMode && folderId != null ? (
          <button
            type="button"
            draggable
            aria-label={`Reorder folder ${title}`}
            className="flex shrink-0 cursor-grab items-center text-muted-foreground hover:text-muted-foreground active:cursor-grabbing dark:hover:text-foreground"
            onDragStart={(event) => {
              beginFolderDrag(event, folderId)
              onFolderDragStart(folderId)
            }}
            onDragEnd={onFolderDragEnd}
          >
            <GripVertical className="size-4" aria-hidden />
          </button>
        ) : null}
        <div className="flex min-w-0 flex-1 items-center gap-1">
          <h3 className="min-w-0 truncate text-sm font-bold text-foreground">
            {title}
          </h3>
          {!editMode && folderName != null ? (
            <CreateServerDialog
              defaultFolderName={folderName}
              trigger={
                <button
                  type="button"
                  aria-label={`Create server in ${title}`}
                  className="flex size-5 shrink-0 items-center justify-center rounded-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                >
                  <Plus className="size-3" />
                </button>
              }
            />
          ) : null}
        </div>
        {editMode && folderId != null ? (
          <div className="flex shrink-0 items-center">
            <RenameFolderDialog folderId={folderId} currentName={title} />
            <DeleteFolderButton folderId={folderId} folderName={title} />
          </div>
        ) : null}
        <CountBadge count={serverIds.length} hideZero={false} />
      </div>

      {serverIds.length > 0 ? (
        <div
          {...(editMode
            ? {
                onDragOver: handleDragOver,
                onDrop: (event: DragEvent<HTMLDivElement>) => {
                  event.stopPropagation()
                  handleDrop(event)
                },
              }
            : undefined)}
        >
          <div className="flex flex-col gap-2 lg:hidden">
            {serverIds.map((serverId) => (
              <ServerCard
                key={serverId}
                server={serversMap[serverId]}
                editMode={editMode}
                isDragging={draggingServerId === serverId}
                onDragStart={() => onServerDragStart(String(serverId))}
                onDragEnd={onServerDragEnd}
              />
            ))}
          </div>
          <div className="hidden lg:block">
            <DataTable
              table={table}
              rowDrag={rowDrag}
              renderRow={(row) => (
                <ServerTableDataRow key={row.id} row={row} rowDrag={rowDrag} />
              )}
            />
          </div>
        </div>
      ) : (
        <p
          className={cn(
            "rounded-sm border border-dashed border-border px-3 py-6 text-center text-sm text-muted-foreground",
            isDropTarget && "border-monitor dark:border-warning"
          )}
          {...(editMode
            ? {
                onDragOver: handleDragOver,
                onDrop: (event: DragEvent<HTMLParagraphElement>) => {
                  event.stopPropagation()
                  handleDrop(event)
                },
              }
            : undefined)}
        >
          {showDropTarget
            ? draggingFolderId != null
              ? "Drop to reorder here"
              : "Drop to move here"
            : "No servers in this folder."}
        </p>
      )}
    </section>
  )
}

const ServersFolderTable = memo(ServersFolderTableInner)

export { ServersFolderTable }
