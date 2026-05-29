//go:build linux

package linux

import (
	"time"

	"fascinated.cc/monitor/agent/internal/iostats"
)

func LookupDiskstatsDelta(device string, before, after map[string]DiskstatsEntry, elapsed time.Duration) (iostats.Rates, bool) {
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
