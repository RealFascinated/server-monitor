import { useCallback } from "react"

import { useUserPreference } from "@/hooks/use-user-preference"
import { Preferences } from "@/lib/preferences"

export const SIDEBAR_COLUMNS = ["cpu", "ram"] as const

export type SidebarColumnId = (typeof SIDEBAR_COLUMNS)[number]

export type SidebarColumnVisibility = Record<SidebarColumnId, boolean>

export function useSidebarColumnVisibility() {
  const { value: cpu, setValue: setCpu } = useUserPreference(
    Preferences.SIDEBAR_COLUMNS_CPU
  )
  const { value: ram, setValue: setRam } = useUserPreference(
    Preferences.SIDEBAR_COLUMNS_RAM
  )

  const visibility: SidebarColumnVisibility = { cpu, ram }

  const setColumnVisible = useCallback(
    (column: SidebarColumnId, visible: boolean) => {
      switch (column) {
        case "cpu": setCpu(visible); break
        case "ram": setRam(visible); break
      }
    },
    [setCpu, setRam]
  )

  return { visibility, setColumnVisible } as const
}
