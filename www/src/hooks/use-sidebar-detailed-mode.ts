import { useCallback, useState } from "react"

import {
  useLocalStorageSync,
  writeLocalStorage,
} from "@/lib/local-storage-sync"

const SIDEBAR_DETAILED_MODE_STORAGE_KEY = "sidebar-detailed-mode"

function readDetailedMode(): boolean {
  return localStorage.getItem(SIDEBAR_DETAILED_MODE_STORAGE_KEY) === "true"
}

export function useSidebarDetailedMode() {
  const [detailed, setDetailed] = useState(readDetailedMode)

  useLocalStorageSync(SIDEBAR_DETAILED_MODE_STORAGE_KEY, () => {
    setDetailed(readDetailedMode())
  })

  const setDetailedPersisted = useCallback((value: boolean) => {
    setDetailed(value)
    writeLocalStorage(SIDEBAR_DETAILED_MODE_STORAGE_KEY, String(value))
  }, [])

  const toggleDetailed = useCallback(() => {
    setDetailed((current) => {
      const next = !current
      writeLocalStorage(SIDEBAR_DETAILED_MODE_STORAGE_KEY, String(next))
      return next
    })
  }, [])

  return {
    detailed,
    setDetailed: setDetailedPersisted,
    toggleDetailed,
  } as const
}
