import { useEffect, useState } from "react"

const DEFAULT_INTERVAL_MS = 30_000

type UseLiveNowOptions = {
  enabled: boolean
  intervalMs?: number
}

export function useLiveNow({
  enabled,
  intervalMs = DEFAULT_INTERVAL_MS,
}: UseLiveNowOptions): number {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!enabled) {
      return
    }

    const interval = window.setInterval(() => {
      setNow(Date.now())
    }, intervalMs)

    return () => {
      window.clearInterval(interval)
    }
  }, [enabled, intervalMs])

  return now
}
