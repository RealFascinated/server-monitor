export const SERVER_DRAG_MIME = "application/x-monitor-server-id"
export const FOLDER_DRAG_MIME = "application/x-monitor-folder-id"

export function readDraggedServerId(dataTransfer: DataTransfer): number | null {
  const raw =
    dataTransfer.getData(SERVER_DRAG_MIME) || dataTransfer.getData("text/plain")
  if (!raw) {
    return null
  }
  const serverId = Number(raw)
  return Number.isFinite(serverId) ? serverId : null
}

export function readDraggedFolderId(dataTransfer: DataTransfer): number | null {
  const raw = dataTransfer.getData(FOLDER_DRAG_MIME)
  if (!raw) {
    return null
  }
  const folderId = Number(raw)
  return Number.isFinite(folderId) ? folderId : null
}

export function computeReorderedFolderIds(
  folderIds: number[],
  draggedFolderId: number,
  targetFolderId: number
): number[] | null {
  const fromIndex = folderIds.indexOf(draggedFolderId)
  const toIndex = folderIds.indexOf(targetFolderId)

  if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) {
    return null
  }

  const next = [...folderIds]
  next.splice(fromIndex, 1)
  next.splice(toIndex, 0, draggedFolderId)
  return next
}
