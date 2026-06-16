import { queryOptions } from "@tanstack/react-query"

import { getUserServerFolders } from "./folders"

export const userServerFoldersQueryKey = ["user", "server-folders"] as const

export function userServerFoldersQueryOptions() {
  return queryOptions({
    queryKey: userServerFoldersQueryKey,
    queryFn: getUserServerFolders,
  })
}
