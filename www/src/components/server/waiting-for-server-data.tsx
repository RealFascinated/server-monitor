import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useEffect } from "react"

import { Callout } from "@/components/callout"
import { Spinner } from "@/components/spinner"
import {
  userServersQueryKey,
  userServerStatusQueryOptions,
} from "@/lib/api/user/servers.queries"

type WaitingForServerDataProps = {
  serverId: number
}

function WaitingForServerData({ serverId }: WaitingForServerDataProps) {
  const queryClient = useQueryClient()
  const { data: status } = useQuery(userServerStatusQueryOptions(serverId))

  const hasMetrics = status?.hasMetrics ?? false

  useEffect(() => {
    if (!hasMetrics) {
      return
    }

    void queryClient.invalidateQueries({ queryKey: userServersQueryKey })
  }, [hasMetrics, queryClient])

  if (hasMetrics) {
    return (
      <Callout type="success" title="Agent connected">
        First metrics received. You can open the server dashboard to explore
        live data.
      </Callout>
    )
  }

  return (
    <Callout type="info" title="Waiting for data">
      <p className="flex items-center gap-2">
        <Spinner />
        Listening for the first report from the Monitor Agent…
      </p>
      {status?.agentVersion ? (
        <p className="mt-2 text-xs text-muted-foreground">
          Agent v{status.agentVersion} connected; waiting for metrics.
        </p>
      ) : null}
    </Callout>
  )
}

export { WaitingForServerData }
