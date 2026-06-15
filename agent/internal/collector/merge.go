package collector

import (
	"fascinated.cc/monitor/agent/internal/ingest"
)

func overlaySlowServerMetrics(dest *ingest.ServerMetrics, src ingest.ServerMetrics) {
	if len(src.TemperatureMetrics) > 0 {
		dest.TemperatureMetrics = src.TemperatureMetrics
	}
	if len(src.CPUCoreMetrics) > 0 {
		dest.CPUCoreMetrics = src.CPUCoreMetrics
	}
	if src.CPUPowerWatts > 0 {
		dest.CPUPowerWatts = src.CPUPowerWatts
	}
	if src.MemoryTotal > 0 {
		dest.MemoryUsage = src.MemoryUsage
		dest.MemoryAvailable = src.MemoryAvailable
		dest.MemoryTotal = src.MemoryTotal
	}
}
