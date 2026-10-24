// Package selfstats reports the agent's own resource usage and push health.
package selfstats

import (
	"runtime"
	"sync/atomic"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
)

var (
	startedAt = time.Now()

	lastPushDurationMs   atomic.Int64
	lastPushPayloadBytes atomic.Int64
	pushFailuresTotal    atomic.Int64
)

// RecordPush stores the outcome of the most recent push attempt.
func RecordPush(duration time.Duration, payloadBytes int, err error) {
	lastPushDurationMs.Store(duration.Milliseconds())
	lastPushPayloadBytes.Store(int64(payloadBytes))
	if err != nil {
		pushFailuresTotal.Add(1)
	}
}

// Collect returns the agent's current resource usage. The lastPush* fields describe the
// previous push, since the current one has not been sent yet.
func Collect() *ingest.AgentMetrics {
	var mem runtime.MemStats
	runtime.ReadMemStats(&mem)

	return &ingest.AgentMetrics{
		RssBytes:             rssBytes(),
		Goroutines:           int64(runtime.NumGoroutine()),
		HeapInUseBytes:       int64(mem.HeapInuse),
		OpenFds:              openFDs(),
		UptimeSeconds:        int64(time.Since(startedAt).Seconds()),
		LastPushDurationMs:   lastPushDurationMs.Load(),
		LastPushPayloadBytes: lastPushPayloadBytes.Load(),
		PushFailuresTotal:    pushFailuresTotal.Load(),
	}
}

// AddBufferStats copies the state of the push buffer into metrics.
func AddBufferStats(metrics *ingest.AgentMetrics, stats ingest.QueueStats) {
	metrics.BufferedSamples = int64(stats.Samples)
	metrics.BufferedBytes = int64(stats.Bytes)
	metrics.BufferOldestAgeSeconds = int64(stats.OldestAge.Seconds())
	metrics.BufferDroppedTotal = stats.DroppedTotal
}
