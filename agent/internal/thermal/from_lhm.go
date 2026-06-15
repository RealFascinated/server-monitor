//go:build windows

package thermal

import (
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/lhm"
)

func FromLHMSnapshot(snap lhm.ServerSnapshot) []ingest.TemperatureMetric {
	if len(snap.Temperatures) == 0 {
		return nil
	}
	return snap.Temperatures
}
