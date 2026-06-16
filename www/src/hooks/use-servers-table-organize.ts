import { useCallback, useEffect, useState } from "react"

import { useMoveServerToFolder } from "@/hooks/use-move-server-to-folder"
import { useReorderServerFolders } from "@/hooks/use-reorder-server-folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import type { ServerResponse } from "@/lib/api/user/servers"
import { computeReorderedFolderIds } from "@/lib/servers/drag"
import { toastMutationError } from "@/lib/toast"

type UseServersTableOrganizeOptions = {
  editMode: boolean
  folders: ServerFolderResponse[]
  serversMap: Record<number, ServerResponse>
}

export function useServersTableOrganize({
  editMode,
  folders,
  serversMap,
}: UseServersTableOrganizeOptions) {
  const [draggingRowId, setDraggingRowId] = useState<string | null>(null)
  const [draggingFolderId, setDraggingFolderId] = useState<number | null>(null)
  const [activeDropTargetKey, setActiveDropTargetKey] = useState<string | null>(
    null
  )

  const moveServer = useMoveServerToFolder()
  const reorderFolders = useReorderServerFolders()

  const draggingServerId = draggingRowId != null ? Number(draggingRowId) : null
  const draggedServerFolderName =
    draggingServerId != null
      ? (serversMap[draggingServerId].folderName ?? null)
      : null

  useEffect(() => {
    if (!editMode) {
      setDraggingRowId(null)
      setDraggingFolderId(null)
      setActiveDropTargetKey(null)
    }
  }, [editMode])

  function canAcceptServerDrop(targetFolderName: string | null) {
    return draggedServerFolderName !== targetFolderName
  }

  const onServerDragStart = useCallback((rowId: string) => {
    setDraggingRowId(rowId)
  }, [])

  const onServerDragEnd = useCallback(() => {
    setDraggingRowId(null)
    setActiveDropTargetKey(null)
  }, [])

  const onFolderDragStart = useCallback((folderId: number) => {
    setDraggingFolderId(folderId)
  }, [])

  const onFolderDragEnd = useCallback(() => {
    setDraggingFolderId(null)
    setActiveDropTargetKey(null)
  }, [])

  const onMoveServer = useCallback(
    (serverId: number, folderName: string | null) => {
      const currentFolder = serversMap[serverId].folderName ?? null
      if (currentFolder === folderName) {
        return
      }

      setDraggingRowId(null)
      setActiveDropTargetKey(null)
      moveServer.mutate(
        { serverId, folderName },
        {
          onError: (moveErr) => {
            toastMutationError(
              "Could not update folders",
              moveErr,
              "Failed to move server"
            )
          },
        }
      )
    },
    [moveServer, serversMap]
  )

  const onReorderFolder = useCallback(
    (draggedFolderId: number, targetFolderId: number) => {
      const folderIds = folders.map((folder) => folder.id)
      const nextFolderIds = computeReorderedFolderIds(
        folderIds,
        draggedFolderId,
        targetFolderId
      )

      if (nextFolderIds == null) {
        return
      }

      setDraggingFolderId(null)
      setActiveDropTargetKey(null)
      reorderFolders.mutate(
        { folderIds: nextFolderIds },
        {
          onError: (reorderErr) => {
            toastMutationError(
              "Could not update folders",
              reorderErr,
              "Failed to reorder folders"
            )
          },
        }
      )
    },
    [folders, reorderFolders]
  )

  return {
    draggingServerId,
    draggingFolderId,
    activeDropTargetKey,
    canAcceptServerDrop,
    onServerDragStart,
    onServerDragEnd,
    onFolderDragStart,
    onFolderDragEnd,
    onDropTargetChange: setActiveDropTargetKey,
    onMoveServer,
    onReorderFolder,
  }
}
