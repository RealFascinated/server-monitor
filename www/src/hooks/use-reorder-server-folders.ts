import { useMutation, useQueryClient } from "@tanstack/react-query"

import { reorderServerFolders } from "@/lib/api/user/folders"
import type { ServerFolderResponse } from "@/lib/api/user/folders"
import { userServerFoldersQueryKey } from "@/lib/api/user/folders.queries"

export function useReorderServerFolders() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: reorderServerFolders,
    onMutate: ({ folderIds }) => {
      void queryClient.cancelQueries({ queryKey: userServerFoldersQueryKey })

      const previous = queryClient.getQueryData<ServerFolderResponse[]>(
        userServerFoldersQueryKey
      )

      if (previous) {
        const foldersById = new Map(
          previous.map((folder) => [folder.id, folder])
        )
        const optimistic = folderIds.flatMap((folderId, position) => {
          const folder = foldersById.get(folderId)
          return folder ? [{ ...folder, position }] : []
        })

        if (optimistic.length === folderIds.length) {
          queryClient.setQueryData(userServerFoldersQueryKey, optimistic)
        }
      }

      return { previous }
    },
    onError: (_error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(userServerFoldersQueryKey, context.previous)
      }
    },
    onSuccess: (folders) => {
      queryClient.setQueryData(userServerFoldersQueryKey, folders)
    },
  })
}
