import { useCallback } from "react"

import { useUserPreference } from "@/hooks/use-user-preference"
import { Preferences } from "@/lib/preferences"

export function useSidebarDetailedMode() {
  const { value: detailed, setValue } = useUserPreference(
    Preferences.SIDEBAR_DETAILED_MODE
  )

  const toggleDetailed = useCallback(() => {
    setValue(!detailed)
  }, [detailed, setValue])

  return {
    detailed,
    setDetailed: setValue,
    toggleDetailed,
  } as const
}
