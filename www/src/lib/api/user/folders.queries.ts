import { queryOptions } from "@tanstack/react-query"

import { getUserServerFolders } from "./folders"

export function userServerFoldersQueryOptions() {
  return queryOptions({
    queryKey: ["user", "server-folders"],
    queryFn: getUserServerFolders,
  })
}
