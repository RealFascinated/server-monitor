//go:build windows

package gpu

import (
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/lhm"
)

func FromLHM(snap lhm.ServerSnapshot) []ingest.GPUMetric {
	if len(snap.GPUs) == 0 {
		return nil
	}
	return snap.GPUs
}
