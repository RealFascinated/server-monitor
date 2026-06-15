//go:build linux

package linux

import (
	"time"

	"fascinated.cc/monitor/agent/internal/iostats"
)

func LookupDiskstatsDelta(device string, before, after map[string]DiskstatsEntry, elapsed time.Duration) (iostats.Rates, bool) {
	devices := diskstatsDevicesForLookup(device, after)
	if len(devices) == 1 {
		return lookupSingleDiskstatsDelta(devices[0], before, after, elapsed)
	}
	return aggregateDiskstatsDelta(devices, before, after, elapsed)
}

func lookupSingleDiskstatsDelta(device string, before, after map[string]DiskstatsEntry, elapsed time.Duration) (iostats.Rates, bool) {
	prev, okBefore := before[device]
	curr, okAfter := after[device]
	if !okBefore || !okAfter {
		return iostats.Rates{}, false
	}

	return iostats.RatesFromDiskstats(
		prev.Reads, prev.SectorsRead, prev.ReadMs, prev.Writes, prev.SectorsWritten, prev.WriteMs, prev.IoMs,
		curr.Reads, curr.SectorsRead, curr.ReadMs, curr.Writes, curr.SectorsWritten, curr.WriteMs, curr.IoMs,
		elapsed,
	), true
}

func aggregateDiskstatsDelta(devices []string, before, after map[string]DiskstatsEntry, elapsed time.Duration) (iostats.Rates, bool) {
	var found bool
	var total iostats.Rates
	var totalReadMs, totalWriteMs float64

	for _, device := range devices {
		rates, ok := lookupSingleDiskstatsDelta(device, before, after, elapsed)
		if !ok {
			continue
		}
		found = true
		total.ReadBytesPerSecond += rates.ReadBytesPerSecond
		total.WriteBytesPerSecond += rates.WriteBytesPerSecond
		total.ReadIops += rates.ReadIops
		total.WriteIops += rates.WriteIops
		totalReadMs += float64(rates.ReadLatencyMs) * float64(rates.ReadIops)
		totalWriteMs += float64(rates.WriteLatencyMs) * float64(rates.WriteIops)
		if rates.IoUsagePercent > total.IoUsagePercent {
			total.IoUsagePercent = rates.IoUsagePercent
		}
		if rates.ReadBytesPerSecond+rates.WriteBytesPerSecond > 0 && rates.IoWaitMilliseconds > total.IoWaitMilliseconds {
			total.IoWaitMilliseconds = rates.IoWaitMilliseconds
		}
	}

	if !found {
		return iostats.Rates{}, false
	}
	if total.ReadIops > 0 {
		total.ReadLatencyMs = int64(totalReadMs/float64(total.ReadIops) + 0.5)
	}
	if total.WriteIops > 0 {
		total.WriteLatencyMs = int64(totalWriteMs/float64(total.WriteIops) + 0.5)
	}
	return total, true
}
