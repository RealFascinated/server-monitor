import { useQuery } from "@tanstack/react-query"

import { userInvitesQueryOptions } from "@/lib/api/user/invites.queries"

export function useUserInvites() {
  return useQuery(userInvitesQueryOptions())
}
