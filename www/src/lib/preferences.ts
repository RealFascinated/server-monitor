import type { MetricRefreshInterval } from "@/lib/metrics/refresh-interval"
import type { MetricTimeRange } from "@/lib/metrics/range"
import { DEFAULT_METRIC_TIME_RANGE } from "@/lib/metrics/range"

type PreferenceDefinition<T extends boolean | string | number> = {
  key: string
  defaultValue: T
}

export const Preferences = {
  METRIC_DEFAULT_RANGE: {
    key: "metric_default_range",
    defaultValue: DEFAULT_METRIC_TIME_RANGE,
  } satisfies PreferenceDefinition<MetricTimeRange>,

  METRIC_REFRESH_INTERVAL: {
    key: "metric_refresh_interval",
    defaultValue: "10s" as MetricRefreshInterval,
  } satisfies PreferenceDefinition<MetricRefreshInterval>,

  SIDEBAR_DETAILED_MODE: {
    key: "sidebar_detailed_mode",
    defaultValue: false as boolean,
  } satisfies PreferenceDefinition<boolean>,

  SIDEBAR_COLUMNS_CPU: {
    key: "sidebar_columns_cpu",
    defaultValue: true as boolean,
  } satisfies PreferenceDefinition<boolean>,

  SIDEBAR_COLUMNS_RAM: {
    key: "sidebar_columns_ram",
    defaultValue: true as boolean,
  } satisfies PreferenceDefinition<boolean>,

  SIDEBAR_WIDTH: {
    key: "sidebar_width",
    defaultValue: 224,
  } satisfies PreferenceDefinition<number>,
} as const
