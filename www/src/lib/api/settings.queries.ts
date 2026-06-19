import { queryOptions } from "@tanstack/react-query"

import { getPublicSettings } from "@/lib/api/settings"

export function publicSettingsQueryOptions() {
  return queryOptions({
    queryKey: ["settings", "public"],
    queryFn: getPublicSettings,
    staleTime: 60_000,
    retry: false,
  })
}
