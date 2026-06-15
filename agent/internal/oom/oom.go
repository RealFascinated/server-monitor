package oom

import (
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
)

func Read() (total uint64, ok bool) {
	return read()
}

func ApplyRate(metrics *ingest.ServerMetrics, prev, curr uint64, hasPrev bool, elapsed time.Duration) {
	if !hasPrev {
		return
	}
	metrics.OomKillsTotal = iostats.Uint64ToInt64(curr)
	metrics.OomKillsPerSecond = iostats.PerSecond(delta.Uint64(curr, prev), elapsed)
}
