package fd

import (
	"math"

	"fascinated.cc/monitor/agent/internal/ingest"
)

type Snapshot struct {
	Open, Max int64
}

func Read() Snapshot {
	return read()
}

func ApplyTo(metrics *ingest.ServerMetrics, s Snapshot) {
	if s.Max <= 0 {
		return
	}
	metrics.FdOpen = s.Open
	if s.Max == math.MaxInt64 {
		return
	}
	metrics.FdMax = s.Max
	metrics.FdUsagePercent = float64(s.Open) / float64(s.Max) * 100
}
