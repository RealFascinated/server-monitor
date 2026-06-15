import type { QueryClient } from "@tanstack/react-query"

import type { ServerFolderResponse } from "@/lib/api/user/folders"

export function invalidateServerFoldersIfNeeded(
  queryClient: QueryClient,
  folderName: string | null
) {
  if (!folderName) {
    return
  }

  const folders = queryClient.getQueryData<ServerFolderResponse[]>([
    "user",
    "server-folders",
  ])

  const alreadyKnown = folders?.some(
    (folder) =>
      folder.name.localeCompare(folderName, undefined, {
        sensitivity: "accent",
      }) === 0
  )

  if (!alreadyKnown) {
    void queryClient.invalidateQueries({
      queryKey: ["user", "server-folders"],
    })
  }
}
