import { queryOptions } from "@tanstack/react-query"

import {
  getServerInvitePreview,
  getUserPendingInvites,
} from "@/lib/api/user/invites"

export const userInvitesQueryKey = ["user", "invites"] as const

export function userInvitesQueryOptions() {
  return queryOptions({
    queryKey: userInvitesQueryKey,
    queryFn: getUserPendingInvites,
  })
}

export function serverInvitePreviewQueryOptions(token: string) {
  return queryOptions({
    queryKey: [...userInvitesQueryKey, "preview", token] as const,
    queryFn: () => getServerInvitePreview(token),
  })
}
