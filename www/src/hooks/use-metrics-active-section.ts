import { useCallback, useEffect, useRef, useState } from "react"

import type { MetricsSectionLeaf } from "@/lib/metrics/sections/types"

const SCROLL_PADDING = 128

function useMetricsActiveSection(
  leaves: MetricsSectionLeaf[],
  sectionIdsKey: string
) {
  const activeIdRef = useRef(leaves[0]?.id ?? "")
  const leavesRef = useRef(leaves)
  const isProgrammaticScrollRef = useRef(false)
  const programmaticScrollTimerRef = useRef<ReturnType<
    typeof setTimeout
  > | null>(null)
  const [activeId, setActiveId] = useState(leaves[0]?.id ?? "")

  leavesRef.current = leaves

  const syncActiveSection = useCallback(() => {
    if (isProgrammaticScrollRef.current) {
      return
    }

    const scrollLine = window.scrollY + SCROLL_PADDING
    let nextId = leavesRef.current[0]?.id ?? ""

    for (const leaf of leavesRef.current) {
      const element = document.querySelector(
        `[data-section-id="${CSS.escape(leaf.id)}"]`
      )
      if (!element) {
        continue
      }

      const top = element.getBoundingClientRect().top + window.scrollY
      if (top <= scrollLine + 1) {
        nextId = leaf.id
      }
    }

    if (nextId === activeIdRef.current) {
      return
    }

    activeIdRef.current = nextId
    setActiveId(nextId)
  }, [])

  useEffect(() => {
    let rafId = 0

    const onScroll = () => {
      if (isProgrammaticScrollRef.current) {
        if (programmaticScrollTimerRef.current) {
          clearTimeout(programmaticScrollTimerRef.current)
        }

        programmaticScrollTimerRef.current = setTimeout(() => {
          programmaticScrollTimerRef.current = null
          isProgrammaticScrollRef.current = false
        }, 150)
        return
      }

      if (rafId) {
        return
      }

      rafId = requestAnimationFrame(() => {
        rafId = 0
        syncActiveSection()
      })
    }

    window.addEventListener("scroll", onScroll, { passive: true })
    syncActiveSection()

    return () => {
      window.removeEventListener("scroll", onScroll)
      if (rafId) {
        cancelAnimationFrame(rafId)
      }
      if (programmaticScrollTimerRef.current) {
        clearTimeout(programmaticScrollTimerRef.current)
      }
    }
  }, [syncActiveSection, sectionIdsKey])

  useEffect(() => {
    const firstId = leaves[0]?.id ?? ""
    activeIdRef.current = firstId
    setActiveId(firstId)
  }, [sectionIdsKey, leaves])

  const scrollToSection = useCallback(
    (id: string, behavior: ScrollBehavior = "smooth") => {
      const element = document.querySelector(
        `[data-section-id="${CSS.escape(id)}"]`
      )
      if (!element) {
        return
      }

      if (programmaticScrollTimerRef.current) {
        clearTimeout(programmaticScrollTimerRef.current)
        programmaticScrollTimerRef.current = null
      }

      isProgrammaticScrollRef.current = true
      activeIdRef.current = id
      setActiveId(id)
      element.scrollIntoView({ behavior, block: "start" })
    },
    []
  )

  return { activeId, scrollToSection }
}

export { useMetricsActiveSection }
