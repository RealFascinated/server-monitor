import { queryOptions } from "@tanstack/react-query"

import { getLatestAgentVersion } from "@/lib/api/agent"

const LATEST_AGENT_VERSION_STALE_MS = 5 * 60_000

export const latestAgentVersionQueryKey = ["agent", "latest-version"] as const

export function latestAgentVersionQueryOptions() {
  return queryOptions({
    queryKey: latestAgentVersionQueryKey,
    queryFn: getLatestAgentVersion,
    staleTime: LATEST_AGENT_VERSION_STALE_MS,
  })
}
