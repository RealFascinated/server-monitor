import type { DragEvent } from "react"

export const SERVER_DRAG_MIME = "application/x-monitor-server-id"
export const FOLDER_DRAG_MIME = "application/x-monitor-folder-id"

function setupDragImage(event: DragEvent<HTMLElement>): void {
  const dataTransfer = event.dataTransfer
  if (!dataTransfer) {
    return
  }

  const element = event.currentTarget
  const rect = element.getBoundingClientRect()
  dataTransfer.setDragImage(element, rect.width / 2, rect.height / 2)
}

export function writeServerDragData(
  dataTransfer: DataTransfer,
  serverId: number
): void {
  dataTransfer.effectAllowed = "move"
  dataTransfer.setData(SERVER_DRAG_MIME, String(serverId))
  dataTransfer.setData("text/plain", String(serverId))
}

export function writeFolderDragData(
  dataTransfer: DataTransfer,
  folderId: number
): void {
  dataTransfer.effectAllowed = "move"
  dataTransfer.setData(FOLDER_DRAG_MIME, String(folderId))
}

export function beginServerDrag(
  event: DragEvent<HTMLElement>,
  serverId: number
): void {
  const dataTransfer = event.dataTransfer
  if (!dataTransfer) {
    return
  }

  writeServerDragData(dataTransfer, serverId)
  setupDragImage(event)
}

export function beginFolderDrag(
  event: DragEvent<HTMLElement>,
  folderId: number
): void {
  const dataTransfer = event.dataTransfer
  if (!dataTransfer) {
    return
  }

  writeFolderDragData(dataTransfer, folderId)
  setupDragImage(event)
}

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
