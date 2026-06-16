import type { QueryClient } from "@tanstack/react-query"

import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServerFoldersQueryKey } from "@/lib/api/user/folders.queries"

export function invalidateServerFoldersIfNeeded(
  queryClient: QueryClient,
  folderName: string | null
) {
  if (!folderName) {
    return
  }

  const folders = queryClient.getQueryData<ServerFolderResponse[]>(
    userServerFoldersQueryKey
  )

  const alreadyKnown = folders?.some(
    (folder) =>
      folder.name.localeCompare(folderName, undefined, {
        sensitivity: "accent",
      }) === 0
  )

  if (!alreadyKnown) {
    void queryClient.invalidateQueries({
      queryKey: userServerFoldersQueryKey,
    })
  }
}
