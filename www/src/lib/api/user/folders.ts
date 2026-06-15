import { apiFetch } from "@/lib/auth/api"

export type ServerFolderResponse = {
  id: number
  name: string
  position: number
}

export type ServerFolderRenameRequest = {
  name: string
}

export type ReorderServerFoldersRequest = {
  folderIds: number[]
}

export type UpdateServerFolderRequest = {
  folderName: string | null
}

export type ServerFolderAssignmentResponse = {
  serverId: number
  folderName: string | null
}

export function getUserServerFolders(): Promise<ServerFolderResponse[]> {
  return apiFetch<ServerFolderResponse[]>("/v1/user/server-folders")
}

export function createServerFolder(
  request: ServerFolderRenameRequest
): Promise<ServerFolderResponse> {
  return apiFetch<ServerFolderResponse>("/v1/user/server-folders", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function renameServerFolder(
  folderId: number,
  request: ServerFolderRenameRequest
): Promise<ServerFolderResponse> {
  return apiFetch<ServerFolderResponse>(
    `/v1/user/server-folders/${folderId}/rename`,
    {
      method: "POST",
      body: JSON.stringify(request),
    }
  )
}

export function deleteServerFolder(folderId: number): Promise<void> {
  return apiFetch<void>(`/v1/user/server-folders/${folderId}`, {
    method: "DELETE",
  })
}

export function reorderServerFolders(
  request: ReorderServerFoldersRequest
): Promise<ServerFolderResponse[]> {
  return apiFetch<ServerFolderResponse[]>("/v1/user/server-folders/reorder", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export function updateServerFolder(
  serverId: number,
  request: UpdateServerFolderRequest
): Promise<ServerFolderAssignmentResponse> {
  return apiFetch<ServerFolderAssignmentResponse>(
    `/v1/servers/${serverId}/folder`,
    {
      method: "PATCH",
      body: JSON.stringify(request),
    }
  )
}
