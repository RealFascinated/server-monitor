import { useMutation, useQueryClient } from "@tanstack/react-query"

import { updateServerFolder } from "@/lib/api/user/folders"
import { invalidateServerFoldersIfNeeded } from "@/lib/servers/folder-queries"
import {
  updateServerFolderNameInCache,
  userServersQueryKey,
} from "@/lib/api/user/servers.queries"
import type { ServerResponse } from "@/lib/api/user/servers"

type MoveServerToFolderInput = {
  serverId: number
  folderName: string | null
}

export function useMoveServerToFolder() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ serverId, folderName }: MoveServerToFolderInput) =>
      updateServerFolder(serverId, { folderName }),
    onMutate: ({ serverId, folderName }) => {
      const servers =
        queryClient.getQueryData<ServerResponse[]>(userServersQueryKey)
      const previousFolderName =
        servers?.find((server) => server.serverId === serverId)?.folderName ??
        null

      updateServerFolderNameInCache(queryClient, serverId, folderName)

      return { serverId, previousFolderName }
    },
    onError: (_error, _variables, context) => {
      if (context) {
        updateServerFolderNameInCache(
          queryClient,
          context.serverId,
          context.previousFolderName
        )
      }
    },
    onSuccess: (assignment) => {
      updateServerFolderNameInCache(
        queryClient,
        assignment.serverId,
        assignment.folderName
      )
      invalidateServerFoldersIfNeeded(queryClient, assignment.folderName)
    },
  })
}
