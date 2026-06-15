package zfs

import (
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
)

type ArcSnapshot struct {
	Size, Target, Min, Max, Data, Metadata, L2, Hits, Misses uint64
}

func ReadArcSnapshot() (ArcSnapshot, bool) {
	stats, err := readArcStats()
	if err != nil {
		return ArcSnapshot{}, false
	}
	return ArcSnapshot{
		Size:     stats["size"],
		Target:   stats["c"],
		Min:      stats["c_min"],
		Max:      stats["c_max"],
		Data:     stats["data_size"],
		Metadata: stats["metadata_size"],
		L2:       stats["l2_size"],
		Hits:     stats["hits"],
		Misses:   stats["misses"],
	}, true
}

func ComputeArcMetrics(before, after ArcSnapshot, elapsed time.Duration) *ingest.ZFSArcMetrics {
	hitsDelta := delta.Uint64(after.Hits, before.Hits)
	missesDelta := delta.Uint64(after.Misses, before.Misses)
	total := hitsDelta + missesDelta
	var hitRatio float64
	if total > 0 {
		hitRatio = float64(hitsDelta) / float64(total) * 100
	}

	return &ingest.ZFSArcMetrics{
		ArcSizeBytes:       iostats.Uint64ToInt64(after.Size),
		ArcTargetBytes:     iostats.Uint64ToInt64(after.Target),
		ArcMaxBytes:        iostats.Uint64ToInt64(after.Max),
		ArcMinBytes:        iostats.Uint64ToInt64(after.Min),
		ArcDataBytes:       iostats.Uint64ToInt64(after.Data),
		ArcMetadataBytes:   iostats.Uint64ToInt64(after.Metadata),
		L2ArcSizeBytes:     iostats.Uint64ToInt64(after.L2),
		ArcHitRatio:        hitRatio,
		ArcMissesPerSecond: iostats.PerSecond(missesDelta, elapsed),
	}
}
