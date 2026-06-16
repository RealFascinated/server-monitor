import { queryOptions } from "@tanstack/react-query"

import { getUserPreferences } from "@/lib/api/user/preferences"

export function userPreferencesQueryOptions() {
  return queryOptions({
    queryKey: ["user", "preferences"],
    queryFn: getUserPreferences,
  })
}
