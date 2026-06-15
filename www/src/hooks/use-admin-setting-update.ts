import { useMutation, useQueryClient } from "@tanstack/react-query"

import type { AdminSettingResponse } from "@/lib/api/admin/settings"
import {
  mergeAdminSettingsCache,
  updateAdminSetting,
} from "@/lib/api/admin/settings"
import { adminSettingsQueryOptions } from "@/lib/api/admin/settings.queries"
import { toastMutationError, toastSuccess } from "@/lib/toast"

type SettingUpdateDefinition = {
  key: string
}

export function useAdminSettingUpdate(definition: SettingUpdateDefinition) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (value: boolean | string | number) =>
      updateAdminSetting(definition.key, { value }),
    onSuccess: (updated) => {
      queryClient.setQueryData<AdminSettingResponse[]>(
        adminSettingsQueryOptions().queryKey,
        (current) => mergeAdminSettingsCache(current, updated)
      )
      toastSuccess("Setting updated")
    },
    onError: (error) => {
      toastMutationError(
        "Could not update setting",
        error,
        "Failed to update setting"
      )
    },
  })

  return { mutation }
}
