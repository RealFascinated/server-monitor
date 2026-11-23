// Package tcpstats reports TCP stack quality counters: retransmissions, listen queue overflows
// and drops, and conntrack table usage. Counters are cumulative, so the per-server rates are
// derived between ticks.
package tcpstats

import (
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
)

// Snapshot holds the cumulative counters and gauges read from one sample. A nil field means the
// counter or file was unavailable, so no metric is reported for it.
type Snapshot struct {
	RetransSegs           *uint64
	ListenOverflows       *uint64
	ListenDrops           *uint64
	ConntrackEntries      *int64
	ConntrackMax          *int64
	ConntrackUsagePercent *float64
}

// Apply copies gauges and computes counter rates into metrics. Rates are omitted on the first
// sample (hasPrev false) and whenever either side lacks the counter.
func Apply(metrics *ingest.ServerMetrics, prev, curr Snapshot, hasPrev bool, elapsed time.Duration) {
	if curr.ConntrackEntries != nil {
		metrics.ConntrackEntries = int64Ptr(*curr.ConntrackEntries)
	}
	if curr.ConntrackMax != nil {
		metrics.ConntrackMax = int64Ptr(*curr.ConntrackMax)
	}
	if curr.ConntrackUsagePercent != nil {
		metrics.ConntrackUsagePercent = float64Ptr(*curr.ConntrackUsagePercent)
	}
	if !hasPrev {
		return
	}
	if prev.RetransSegs != nil && curr.RetransSegs != nil {
		metrics.TCPRetransSegmentsPerSecond = int64Ptr(iostats.PerSecond(delta.Uint64(*curr.RetransSegs, *prev.RetransSegs), elapsed))
	}
	if prev.ListenOverflows != nil && curr.ListenOverflows != nil {
		metrics.TCPListenOverflowsPerSecond = int64Ptr(iostats.PerSecond(delta.Uint64(*curr.ListenOverflows, *prev.ListenOverflows), elapsed))
	}
	if prev.ListenDrops != nil && curr.ListenDrops != nil {
		metrics.TCPListenDropsPerSecond = int64Ptr(iostats.PerSecond(delta.Uint64(*curr.ListenDrops, *prev.ListenDrops), elapsed))
	}
}

func int64Ptr(v int64) *int64       { return &v }
func float64Ptr(v float64) *float64 { return &v }
