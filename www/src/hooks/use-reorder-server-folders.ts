import { useMutation, useQueryClient } from "@tanstack/react-query"

import { reorderServerFolders } from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"

export function useReorderServerFolders() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: reorderServerFolders,
    onMutate: ({ folderIds }) => {
      void queryClient.cancelQueries({ queryKey: ["user", "server-folders"] })

      const previous = queryClient.getQueryData<ServerFolderResponse[]>([
        "user",
        "server-folders",
      ])

      if (previous) {
        const foldersById = new Map(
          previous.map((folder) => [folder.id, folder])
        )
        const optimistic = folderIds.flatMap((folderId, position) => {
          const folder = foldersById.get(folderId)
          return folder ? [{ ...folder, position }] : []
        })

        if (optimistic.length === folderIds.length) {
          queryClient.setQueryData(["user", "server-folders"], optimistic)
        }
      }

      return { previous }
    },
    onError: (_error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(["user", "server-folders"], context.previous)
      }
    },
    onSuccess: (folders) => {
      queryClient.setQueryData(["user", "server-folders"], folders)
    },
  })
}
