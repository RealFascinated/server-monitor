//go:build windows

package cpu

import (
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/lhm"
)

func OverlayFromLHM(metrics *ingest.ServerMetrics, snap lhm.ServerSnapshot) {
	if snap.CPUPowerWatts != nil && *snap.CPUPowerWatts > 0 {
		metrics.CPUPowerWatts = *snap.CPUPowerWatts
	}
	if len(snap.Cores) > 0 {
		metrics.CPUCoreMetrics = snap.Cores
	}
}
