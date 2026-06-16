import { useCallback, useEffect, useRef, useState } from "react"
import type { PointerEvent as ReactPointerEvent } from "react"

import { useUserPreference } from "@/hooks/use-user-preference"
import { Preferences } from "@/lib/preferences"

export const SIDEBAR_COMPACT_WIDTH = 56
export const SIDEBAR_DEFAULT_WIDTH = 224
export const SIDEBAR_MIN_WIDTH = 160
export const SIDEBAR_MAX_WIDTH = 400
export const SIDEBAR_LABEL_MIN_WIDTH = 120

function clampWidth(width: number) {
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_COMPACT_WIDTH, width))
}

export function useSidebarWidth() {
  const { value: storedWidth, setValue: persistWidth } = useUserPreference(
    Preferences.SIDEBAR_WIDTH
  )

  const [width, setWidth] = useState(SIDEBAR_DEFAULT_WIDTH)
  const [isResizing, setIsResizing] = useState(false)
  const lastExpandedWidthRef = useRef(SIDEBAR_DEFAULT_WIDTH)
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    const clamped = clampWidth(storedWidth)
    setWidth(clamped)
    if (clamped > SIDEBAR_COMPACT_WIDTH) {
      lastExpandedWidthRef.current = clamped
    }
  }, [storedWidth])

  const setWidthPersisted = useCallback(
    (nextWidth: number) => {
      const clamped = clampWidth(nextWidth)
      setWidth(clamped)
      if (clamped > SIDEBAR_COMPACT_WIDTH) {
        lastExpandedWidthRef.current = clamped
      }
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)
      debounceTimerRef.current = setTimeout(() => persistWidth(clamped), 500)
    },
    [persistWidth]
  )

  const compact = width < SIDEBAR_LABEL_MIN_WIDTH
  const collapsed = width === SIDEBAR_COMPACT_WIDTH

  const toggleCollapsed = useCallback(() => {
    if (collapsed) {
      setWidthPersisted(lastExpandedWidthRef.current)
      return
    }

    if (width > SIDEBAR_COMPACT_WIDTH) {
      lastExpandedWidthRef.current = width
    }
    setWidthPersisted(SIDEBAR_COMPACT_WIDTH)
  }, [collapsed, setWidthPersisted, width])

  const startResize = useCallback(
    (event: ReactPointerEvent<HTMLDivElement>) => {
      event.preventDefault()
      setIsResizing(true)

      const startX = event.clientX
      const startWidth = width

      function onPointerMove(moveEvent: PointerEvent) {
        setWidthPersisted(startWidth + moveEvent.clientX - startX)
      }

      function onPointerUp() {
        setIsResizing(false)
        document.body.style.removeProperty("cursor")
        document.body.style.removeProperty("user-select")
        window.removeEventListener("pointermove", onPointerMove)
        window.removeEventListener("pointerup", onPointerUp)
      }

      document.body.style.cursor = "col-resize"
      document.body.style.userSelect = "none"
      window.addEventListener("pointermove", onPointerMove)
      window.addEventListener("pointerup", onPointerUp)
    },
    [setWidthPersisted, width]
  )

  return {
    width,
    compact,
    collapsed,
    isResizing,
    toggleCollapsed,
    startResize,
  } as const
}
