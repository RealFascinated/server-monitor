import { queryOptions } from "@tanstack/react-query"

import { listUserSessions } from "@/lib/api/user/sessions"

export const userSessionsQueryKey = ["user", "sessions"] as const

export function userSessionsQueryOptions() {
  return queryOptions({
    queryKey: userSessionsQueryKey,
    queryFn: listUserSessions,
  })
}
