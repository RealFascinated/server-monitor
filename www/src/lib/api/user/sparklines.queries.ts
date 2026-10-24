import { queryOptions } from "@tanstack/react-query"

import { getUserServerMetrics } from "@/lib/api/user/metrics"
import { userServerQueryKey } from "@/lib/api/user/servers.queries"
import { buildServerSparklines } from "@/lib/metrics/sparkline"

const SPARKLINE_REFRESH_MS = 60_000

export function userServerSparklineQueryKey(serverId: number) {
  return [...userServerQueryKey(serverId), "sparkline", "1h"] as const
}

/**
 * Last-hour CPU and memory trend for one server. The full metrics response is
 * reduced to a few dozen points inside the query function so only the small
 * result is cached.
 */
export function userServerSparklineQueryOptions(serverId: number) {
  return queryOptions({
    queryKey: userServerSparklineQueryKey(serverId),
    queryFn: async () =>
      buildServerSparklines(
        await getUserServerMetrics(serverId, { kind: "preset", range: "1h" })
      ),
    staleTime: SPARKLINE_REFRESH_MS / 2,
    refetchInterval: SPARKLINE_REFRESH_MS,
  })
}
