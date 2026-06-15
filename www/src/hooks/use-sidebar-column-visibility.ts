import { useCallback, useState } from "react"

import {
  useLocalStorageSync,
  writeLocalStorage,
} from "@/lib/local-storage-sync"

export const SIDEBAR_COLUMNS = ["cpu", "ram"] as const

export type SidebarColumnId = (typeof SIDEBAR_COLUMNS)[number]

export type SidebarColumnVisibility = Record<SidebarColumnId, boolean>

const SIDEBAR_COLUMN_VISIBILITY_STORAGE_KEY = "sidebar-column-visibility"

const DEFAULT_VISIBILITY: SidebarColumnVisibility = {
  cpu: true,
  ram: true,
}

function parseStoredVisibility(stored: string | null): SidebarColumnVisibility {
  if (!stored) {
    return DEFAULT_VISIBILITY
  }

  try {
    const parsed = JSON.parse(stored) as Partial<SidebarColumnVisibility>
    return {
      cpu: parsed.cpu ?? true,
      ram: parsed.ram ?? true,
    }
  } catch {
    return DEFAULT_VISIBILITY
  }
}

function readColumnVisibility(): SidebarColumnVisibility {
  return parseStoredVisibility(
    localStorage.getItem(SIDEBAR_COLUMN_VISIBILITY_STORAGE_KEY)
  )
}

export function useSidebarColumnVisibility() {
  const [visibility, setVisibility] =
    useState<SidebarColumnVisibility>(readColumnVisibility)

  useLocalStorageSync(SIDEBAR_COLUMN_VISIBILITY_STORAGE_KEY, () => {
    setVisibility(readColumnVisibility())
  })

  const setColumnVisible = useCallback(
    (column: SidebarColumnId, visible: boolean) => {
      setVisibility((current) => {
        const next = { ...current, [column]: visible }
        writeLocalStorage(
          SIDEBAR_COLUMN_VISIBILITY_STORAGE_KEY,
          JSON.stringify(next)
        )
        return next
      })
    },
    []
  )

  return {
    visibility,
    setColumnVisible,
  } as const
}
