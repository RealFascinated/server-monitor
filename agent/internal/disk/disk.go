package disk

import (
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
	"fascinated.cc/monitor/agent/internal/zfs"
)

type Mount struct {
	Name       string
	Source     string
	Fstype     string
	DiskType   string
	UsedBytes  uint64
	TotalBytes uint64
	InodeUsed  uint64
	InodeTotal uint64
}

func BuildFromSamples(
	hasZFS bool,
	mounts []Mount,
	beforeIO, afterIO map[string]IOCounters,
	beforeZFS, afterZFS map[string]zfs.PoolIO,
	elapsed time.Duration,
) []ingest.DiskMetric {
	metrics := make([]ingest.DiskMetric, 0, len(mounts))
	for _, mount := range mounts {
		metric := ingest.DiskMetric{
			DiskName:   mount.Name,
			UsedBytes:  int64(mount.UsedBytes),
			TotalBytes: int64(mount.TotalBytes),
			InodeUsed:  int64(mount.InodeUsed),
			InodeTotal: int64(mount.InodeTotal),
		}

		rates := ratesForMount(mount, beforeIO, afterIO, beforeZFS, afterZFS, hasZFS, elapsed)
		metric.IoReadBytesPerSecond = rates.ReadBytesPerSecond
		metric.IoWriteBytesPerSecond = rates.WriteBytesPerSecond
		metric.IoUsagePercent = rates.IoUsagePercent
		metric.IoWaitMilliseconds = rates.IoWaitMilliseconds
		metric.ReadIops = rates.ReadIops
		metric.WriteIops = rates.WriteIops
		metric.ReadLatencyMs = rates.ReadLatencyMs
		metric.WriteLatencyMs = rates.WriteLatencyMs
		metrics = append(metrics, metric)
	}
	return metrics
}

func ratesForMount(
	mount Mount,
	beforeIO, afterIO map[string]IOCounters,
	beforeZFS, afterZFS map[string]zfs.PoolIO,
	hasZFS bool,
	elapsed time.Duration,
) Rates {
	switch mount.DiskType {
	case "zfs":
		if !hasZFS || !zfs.MountGetsPoolIO(mount.Source, mount.Name) {
			return Rates{}
		}
		pool := zfs.PoolName(mount.Source)
		return poolRatesFromSnapshots(pool, beforeZFS, afterZFS, elapsed)
	default:
		device := resolveDiskDevice(mount.Source, beforeIO, afterIO)
		if device == "" {
			return Rates{}
		}
		before, okBefore := beforeIO[device]
		after, okAfter := afterIO[device]
		if !okBefore || !okAfter {
			return Rates{}
		}
		return ratesFromCounters(before, after, elapsed)
	}
}

func poolRatesFromSnapshots(pool string, before, after map[string]zfs.PoolIO, elapsed time.Duration) Rates {
	prev, okBefore := before[pool]
	curr, okAfter := after[pool]
	if !okBefore || !okAfter {
		return Rates{}
	}
	return Rates{
		ReadBytesPerSecond:  iostats.PerSecond(delta.Uint64(curr.Nread, prev.Nread), elapsed),
		WriteBytesPerSecond: iostats.PerSecond(delta.Uint64(curr.Nwritten, prev.Nwritten), elapsed),
	}
}
