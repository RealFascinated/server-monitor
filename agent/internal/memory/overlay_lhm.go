//go:build windows

package memory

import (
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/lhm"
)

func OverlayFromLHM(metrics *ingest.ServerMetrics, snap lhm.ServerSnapshot) {
	if !snap.Memory.Complete() {
		return
	}
	metrics.MemoryUsage = float64(*snap.Memory.Used)
	metrics.MemoryAvailable = float64(*snap.Memory.Available)
	metrics.MemoryTotal = float64(*snap.Memory.Total)
}
