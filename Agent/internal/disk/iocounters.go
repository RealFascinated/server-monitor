package disk

import (
	"time"

	"fascinated.cc/monitor/agent/internal/iostats"
)

type Rates = iostats.Rates

type IOCounters struct {
	ReadBytes  uint64
	WriteBytes uint64
	ReadCount  uint64
	WriteCount uint64
	ReadTime   uint64
	WriteTime  uint64
	IoTime     uint64
}

func ratesFromCounters(before, after IOCounters, elapsed time.Duration) Rates {
	return iostats.RatesFromIO(
		iostats.IOCounts{
			ReadBytes:  before.ReadBytes,
			WriteBytes: before.WriteBytes,
			ReadCount:  before.ReadCount,
			WriteCount: before.WriteCount,
			ReadTime:   before.ReadTime,
			WriteTime:  before.WriteTime,
			IoTime:     before.IoTime,
		},
		iostats.IOCounts{
			ReadBytes:  after.ReadBytes,
			WriteBytes: after.WriteBytes,
			ReadCount:  after.ReadCount,
			WriteCount: after.WriteCount,
			ReadTime:   after.ReadTime,
			WriteTime:  after.WriteTime,
			IoTime:     after.IoTime,
		},
		elapsed,
	)
}

func hasDiskDevice(name string, maps ...map[string]IOCounters) bool {
	for _, m := range maps {
		if _, ok := m[name]; ok {
			return true
		}
	}
	return false
}
