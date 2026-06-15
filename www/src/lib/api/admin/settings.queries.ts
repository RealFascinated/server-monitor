import { queryOptions } from "@tanstack/react-query"

import { getAdminSettings } from "@/lib/api/admin/settings"

export function adminSettingsQueryOptions() {
  return queryOptions({
    queryKey: ["admin", "settings"],
    queryFn: getAdminSettings,
  })
}
