package zfs

import (
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/iostats"
)

func ComputePoolIORates(before, after map[string]PoolIO, elapsed time.Duration) map[string]PoolIORates {
	rates := make(map[string]PoolIORates)
	for pool, curr := range after {
		prev, ok := before[pool]
		if !ok {
			continue
		}
		rates[pool] = PoolIORates{
			ReadBytesPerSecond:  iostats.PerSecond(delta.Uint64(curr.Nread, prev.Nread), elapsed),
			WriteBytesPerSecond: iostats.PerSecond(delta.Uint64(curr.Nwritten, prev.Nwritten), elapsed),
			ReadIops:            iostats.PerSecond(delta.Uint64(curr.Reads, prev.Reads), elapsed),
			WriteIops:           iostats.PerSecond(delta.Uint64(curr.Writes, prev.Writes), elapsed),
		}
	}
	return rates
}
