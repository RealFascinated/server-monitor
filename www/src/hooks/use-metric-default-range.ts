import { useCallback, useState } from "react"

import {
  useLocalStorageSync,
  writeLocalStorage,
} from "@/lib/local-storage-sync"
import {
  DEFAULT_METRIC_TIME_RANGE,
  getStoredDefaultMetricRange,
  METRIC_DEFAULT_RANGE_STORAGE_KEY,
} from "@/lib/metrics/default-range"
import type { MetricTimeRange } from "@/lib/metrics/range"

export function useMetricDefaultRange() {
  const [defaultRange, setDefaultRangeState] = useState<MetricTimeRange>(() =>
    getStoredDefaultMetricRange()
  )

  useLocalStorageSync(METRIC_DEFAULT_RANGE_STORAGE_KEY, () => {
    setDefaultRangeState(getStoredDefaultMetricRange())
  })

  const setDefaultRange = useCallback((value: MetricTimeRange) => {
    setDefaultRangeState(value)
    writeLocalStorage(METRIC_DEFAULT_RANGE_STORAGE_KEY, value)
  }, [])

  return {
    defaultRange,
    setDefaultRange,
  } as const
}
