import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "@tanstack/react-router"

import { useMetricDefaultRange } from "@/hooks/use-metric-default-range"
import type { ServerMemberResponse } from "@/lib/api/user/invites"
import { userInvitesQueryKey } from "@/lib/api/user/invites.queries"
import { userServersQueryKey } from "@/lib/api/user/servers.queries"
import { toastMutationError } from "@/lib/toast"

type UseAcceptServerInviteOptions = {
  mutationFn: () => Promise<ServerMemberResponse>
  errorServerName?: string
}

export function useAcceptServerInvite({
  mutationFn,
  errorServerName,
}: UseAcceptServerInviteOptions) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { defaultRange } = useMetricDefaultRange()

  return useMutation({
    mutationFn,
    onSuccess: async (member) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: userServersQueryKey }),
        queryClient.invalidateQueries({ queryKey: userInvitesQueryKey }),
      ])
      await navigate({
        to: "/servers/$serverId",
        params: { serverId: String(member.serverId) },
        search: { range: defaultRange },
      })
    },
    onError: (mutationError) => {
      toastMutationError(
        errorServerName
          ? `Could not accept invite to ${errorServerName}`
          : "Could not accept invite",
        mutationError,
        "Failed to accept invite"
      )
    },
  })
}
