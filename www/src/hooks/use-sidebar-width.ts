import { useCallback, useEffect, useRef, useState } from "react"
import type { PointerEvent as ReactPointerEvent } from "react"

const SIDEBAR_WIDTH_STORAGE_KEY = "sidebar-width"
const LEGACY_COLLAPSED_STORAGE_KEY = "sidebar-collapsed"

export const SIDEBAR_COMPACT_WIDTH = 56
export const SIDEBAR_DEFAULT_WIDTH = 224
export const SIDEBAR_MIN_WIDTH = 160
export const SIDEBAR_MAX_WIDTH = 400
export const SIDEBAR_LABEL_MIN_WIDTH = 120

function clampWidth(width: number) {
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_COMPACT_WIDTH, width))
}

function readStoredWidth(): number {
  const storedWidth = localStorage.getItem(SIDEBAR_WIDTH_STORAGE_KEY)
  if (storedWidth) {
    const parsed = Number(storedWidth)
    if (!Number.isNaN(parsed)) {
      return clampWidth(parsed)
    }
  }

  const legacyCollapsed = localStorage.getItem(LEGACY_COLLAPSED_STORAGE_KEY)
  if (legacyCollapsed === "true") {
    return SIDEBAR_COMPACT_WIDTH
  }

  return SIDEBAR_DEFAULT_WIDTH
}

export function useSidebarWidth() {
  const [width, setWidth] = useState(SIDEBAR_DEFAULT_WIDTH)
  const [isResizing, setIsResizing] = useState(false)
  const lastExpandedWidthRef = useRef(SIDEBAR_DEFAULT_WIDTH)

  useEffect(() => {
    const storedWidth = readStoredWidth()
    setWidth(storedWidth)
    if (storedWidth > SIDEBAR_COMPACT_WIDTH) {
      lastExpandedWidthRef.current = storedWidth
    }
  }, [])

  const setWidthPersisted = useCallback((nextWidth: number) => {
    const clamped = clampWidth(nextWidth)
    setWidth(clamped)
    localStorage.setItem(SIDEBAR_WIDTH_STORAGE_KEY, String(clamped))
    if (clamped > SIDEBAR_COMPACT_WIDTH) {
      lastExpandedWidthRef.current = clamped
    }
  }, [])

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
