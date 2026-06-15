import { useCallback, useState } from "react"

import {
  useLocalStorageSync,
  writeLocalStorage,
} from "@/lib/local-storage-sync"
import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import { parseMetricRefreshInterval } from "@/lib/metrics/refresh-interval"

const METRIC_REFRESH_INTERVAL_STORAGE_KEY = "metric-refresh-interval"

function readRefreshInterval(): MetricRefreshInterval {
  const stored = localStorage.getItem(METRIC_REFRESH_INTERVAL_STORAGE_KEY)
  return stored ? parseMetricRefreshInterval(stored) : "10s"
}

export function useMetricRefreshInterval() {
  const [refreshInterval, setRefreshIntervalState] =
    useState<MetricRefreshInterval>(readRefreshInterval)

  useLocalStorageSync(METRIC_REFRESH_INTERVAL_STORAGE_KEY, () => {
    setRefreshIntervalState(readRefreshInterval())
  })

  const setRefreshInterval = useCallback((value: MetricRefreshInterval) => {
    setRefreshIntervalState(value)
    writeLocalStorage(METRIC_REFRESH_INTERVAL_STORAGE_KEY, value)
  }, [])

  return {
    refreshInterval,
    setRefreshInterval,
  } as const
}
