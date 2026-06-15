//go:build linux

package disk

import (
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
	"fascinated.cc/monitor/agent/internal/zfs"
)

func BuildLinuxMetrics(
	mounts []Mount,
	diskstatsBefore, diskstatsAfter map[string]linux.DiskstatsEntry,
	cgroupIOBefore, cgroupIOAfter map[string]linux.CgroupIOEntry,
	zfsIOBefore, zfsIOAfter map[string]zfs.PoolIO,
	vdevMap map[string][]string,
	cgroupDevice string,
	hasZFS bool,
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

		rates := Rates{}
		switch mount.DiskType {
		case "zfs":
			if hasZFS && zfs.MountGetsPoolIO(mount.Source, mount.Name) {
				pool := zfs.PoolName(mount.Source)
				rates = poolRatesFromSnapshots(pool, zfsIOBefore, zfsIOAfter, elapsed)
				if vdevRates, ok := lookupZFSPoolVdevDiskStats(pool, diskstatsBefore, diskstatsAfter, vdevMap, elapsed); ok {
					rates.IoUsagePercent = vdevRates.IoUsagePercent
					rates.IoWaitMilliseconds = vdevRates.IoWaitMilliseconds
					if vdevRates.ReadIops+vdevRates.WriteIops > 0 {
						rates.ReadIops = vdevRates.ReadIops
						rates.WriteIops = vdevRates.WriteIops
						rates.ReadLatencyMs = vdevRates.ReadLatencyMs
						rates.WriteLatencyMs = vdevRates.WriteLatencyMs
					}
				}
			}
		case "block":
			device := linux.ResolveDiskstatsDeviceName(mount.Source, diskstatsAfter)
			if device != "" {
				if deviceRates, ok := linux.LookupDiskstatsDelta(device, diskstatsBefore, diskstatsAfter, elapsed); ok {
					rates = deviceRates
				}
			}
		default:
			if len(cgroupIOBefore) > 0 && len(cgroupIOAfter) > 0 {
				readBps, writeBps := linux.LookupCgroupIOBps(cgroupIOBefore, cgroupIOAfter, elapsed)
				rates.ReadBytesPerSecond = readBps
				rates.WriteBytesPerSecond = writeBps
			} else if cgroupDevice != "" {
				if deviceRates, ok := linux.LookupDiskstatsDelta(cgroupDevice, diskstatsBefore, diskstatsAfter, elapsed); ok {
					rates = deviceRates
				}
			}
		}

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

func lookupZFSPoolVdevDiskStats(pool string, before, after map[string]linux.DiskstatsEntry, vdevMap map[string][]string, elapsed time.Duration) (Rates, bool) {
	vdevs := vdevMap[pool]
	if len(vdevs) == 0 {
		return Rates{}, false
	}

	var found bool
	var maxUsage, maxWait float64
	var totalReadIops, totalWriteIops int64
	var totalReadMs, totalWriteMs float64

	for _, vdev := range vdevs {
		rates, ok := linux.LookupDiskstatsDelta(vdev, before, after, elapsed)
		if !ok {
			continue
		}
		found = true
		totalReadIops += rates.ReadIops
		totalWriteIops += rates.WriteIops
		totalReadMs += float64(rates.ReadLatencyMs) * float64(rates.ReadIops)
		totalWriteMs += float64(rates.WriteLatencyMs) * float64(rates.WriteIops)
		if rates.IoUsagePercent > maxUsage {
			maxUsage = rates.IoUsagePercent
		}
		if rates.ReadBytesPerSecond+rates.WriteBytesPerSecond > 0 && rates.IoWaitMilliseconds > maxWait {
			maxWait = rates.IoWaitMilliseconds
		}
	}

	if !found {
		return Rates{}, false
	}

	var readLatency, writeLatency float64
	if totalReadIops > 0 {
		readLatency = totalReadMs / float64(totalReadIops)
	}
	if totalWriteIops > 0 {
		writeLatency = totalWriteMs / float64(totalWriteIops)
	}

	return Rates{
		IoUsagePercent:     maxUsage,
		IoWaitMilliseconds: maxWait,
		ReadIops:             totalReadIops,
		WriteIops:            totalWriteIops,
		ReadLatencyMs:        int64(readLatency + 0.5),
		WriteLatencyMs:       int64(writeLatency + 0.5),
	}, true
}
