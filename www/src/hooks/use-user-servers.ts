import { useQuery } from "@tanstack/react-query"

import { userServersQueryOptions } from "@/lib/api/user/servers.queries"

export function useUserServers() {
  return useQuery(userServersQueryOptions())
}
