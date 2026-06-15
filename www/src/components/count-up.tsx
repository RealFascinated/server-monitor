import { memo, useCallback, useEffect, useRef, useState } from "react"

import { usePrefersReducedMotion } from "@/hooks/use-prefers-reduced-motion"
import { cn } from "@/lib/utils"

type CountUpProps = {
  value: number
  format?: (value: number) => string
  duration?: number
  updateDuration?: number
  className?: string
}

const ENTRANCE_DURATION_MS = 900
const UPDATE_DURATION_MS = 200

function easeOutExpo(progress: number): number {
  return progress === 1 ? 1 : 1 - 2 ** (-10 * progress)
}

function runCountUp(
  from: number,
  to: number,
  duration: number,
  onFrame: (value: number) => void,
  onDone?: () => void
) {
  const start = performance.now()
  let frame = 0

  const tick = (now: number) => {
    const progress = Math.min((now - start) / duration, 1)
    const next = from + (to - from) * easeOutExpo(progress)
    onFrame(next)
    if (progress < 1) {
      frame = requestAnimationFrame(tick)
    } else {
      onFrame(to)
      onDone?.()
    }
  }

  frame = requestAnimationFrame(tick)
  return () => cancelAnimationFrame(frame)
}

const CountUp = memo(function CountUp({
  value,
  format = String,
  duration = ENTRANCE_DURATION_MS,
  updateDuration = UPDATE_DURATION_MS,
  className,
}: CountUpProps) {
  const prefersReducedMotion = usePrefersReducedMotion()
  const [display, setDisplay] = useState(0)
  const displayRef = useRef(0)
  const valueRef = useRef(value)
  const [entranceDone, setEntranceDone] = useState(prefersReducedMotion)

  valueRef.current = value

  const applyDisplay = useCallback((next: number) => {
    displayRef.current = next
    setDisplay(next)
  }, [])

  useEffect(() => {
    if (prefersReducedMotion) {
      applyDisplay(valueRef.current)
      setEntranceDone(true)
      return
    }

    applyDisplay(0)

    return runCountUp(0, valueRef.current, duration, applyDisplay, () => {
      applyDisplay(valueRef.current)
      setEntranceDone(true)
    })
  }, [applyDisplay, duration, prefersReducedMotion])

  useEffect(() => {
    if (!entranceDone) {
      return
    }

    if (prefersReducedMotion) {
      applyDisplay(value)
      return
    }

    const from = displayRef.current
    if (from === value) {
      return
    }

    return runCountUp(from, value, updateDuration, applyDisplay)
  }, [value, entranceDone, prefersReducedMotion, updateDuration, applyDisplay])

  return <span className={cn(className)}>{format(display)}</span>
})

export { CountUp }
