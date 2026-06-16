import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import type { UserPreferenceResponse } from "@/lib/api/user/preferences"
import { updateUserPreference } from "@/lib/api/user/preferences"
import { userPreferencesQueryOptions } from "@/lib/api/user/preferences.queries"

export function useUserPreference<T extends boolean | string | number>(definition: {
  key: string
  defaultValue: T
}): { value: T; setValue: (value: T) => void } {
  const queryClient = useQueryClient()
  const { data: preferences } = useQuery(userPreferencesQueryOptions())

  const stored = preferences?.find((p) => p.key === definition.key)
  const value = stored !== undefined ? (stored.value as T) : definition.defaultValue

  const { mutate } = useMutation({
    mutationFn: (next: T) => updateUserPreference(definition.key, next),
    onSuccess: (updated) => {
      queryClient.setQueryData<UserPreferenceResponse[]>(
        userPreferencesQueryOptions().queryKey,
        (current = []) => {
          const index = current.findIndex((p) => p.key === updated.key)
          if (index === -1) return [...current, updated]
          return current.map((p, i) => (i === index ? updated : p))
        }
      )
    },
  })

  return { value, setValue: mutate }
}
