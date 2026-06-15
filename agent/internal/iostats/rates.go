package iostats

import (
	"math"
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
)

type Rates struct {
	ReadBytesPerSecond  int64
	WriteBytesPerSecond int64
	IoUsagePercent      float64
	IoWaitMilliseconds  float64
	ReadIops            int64
	WriteIops           int64
	ReadLatencyMs       int64
	WriteLatencyMs      int64
}

type IOCounts struct {
	ReadBytes, WriteBytes uint64
	ReadCount, WriteCount uint64
	ReadTime, WriteTime   uint64
	IoTime                uint64
}

// Uint64ToInt64 converts v to int64, capping at math.MaxInt64.
func Uint64ToInt64(v uint64) int64 {
	if v > uint64(math.MaxInt64) {
		return math.MaxInt64
	}
	return int64(v)
}

func PerSecond(count uint64, elapsed time.Duration) int64 {
	if elapsed <= 0 {
		return Uint64ToInt64(count)
	}
	rate := float64(count) / elapsed.Seconds()
	if rate > float64(math.MaxInt64) {
		return math.MaxInt64
	}
	return int64(rate)
}

func PerSecondFloat(count float64, elapsed time.Duration) float64 {
	if elapsed <= 0 {
		return count
	}
	return count / elapsed.Seconds()
}

func RatesFromIO(before, after IOCounts, elapsed time.Duration) Rates {
	readBytes := delta.Uint64(after.ReadBytes, before.ReadBytes)
	writeBytes := delta.Uint64(after.WriteBytes, before.WriteBytes)
	readIops := delta.Uint64(after.ReadCount, before.ReadCount)
	writeIops := delta.Uint64(after.WriteCount, before.WriteCount)
	readMs := delta.Uint64(after.ReadTime, before.ReadTime)
	writeMs := delta.Uint64(after.WriteTime, before.WriteTime)
	ioMs := delta.Uint64(after.IoTime, before.IoTime)

	ioUsage := PerSecondFloat(float64(ioMs)/10.0, elapsed)
	if ioUsage > 100 {
		ioUsage = 100
	}

	var ioWait, readLatency, writeLatency float64
	totalIops := readIops + writeIops
	if totalIops > 0 {
		ioWait = PerSecondFloat(float64(readMs+writeMs), elapsed) / float64(totalIops)
	}
	if readIops > 0 {
		readLatency = PerSecondFloat(float64(readMs), elapsed) / float64(readIops)
	}
	if writeIops > 0 {
		writeLatency = PerSecondFloat(float64(writeMs), elapsed) / float64(writeIops)
	}

	return Rates{
		ReadBytesPerSecond:  PerSecond(readBytes, elapsed),
		WriteBytesPerSecond: PerSecond(writeBytes, elapsed),
		IoUsagePercent:      ioUsage,
		IoWaitMilliseconds:  ioWait,
		ReadIops:            PerSecond(readIops, elapsed),
		WriteIops:           PerSecond(writeIops, elapsed),
		ReadLatencyMs:       int64(math.Round(readLatency)),
		WriteLatencyMs:      int64(math.Round(writeLatency)),
	}
}

func RatesFromDiskstats(
	beforeReads, beforeSectorsRead, beforeReadMs, beforeWrites, beforeSectorsWritten, beforeWriteMs, beforeIoMs uint64,
	afterReads, afterSectorsRead, afterReadMs, afterWrites, afterSectorsWritten, afterWriteMs, afterIoMs uint64,
	elapsed time.Duration,
) Rates {
	return RatesFromIO(
		IOCounts{
			ReadBytes:  beforeSectorsRead * 512,
			WriteBytes: beforeSectorsWritten * 512,
			ReadCount:  beforeReads,
			WriteCount: beforeWrites,
			ReadTime:   beforeReadMs,
			WriteTime:  beforeWriteMs,
			IoTime:     beforeIoMs,
		},
		IOCounts{
			ReadBytes:  afterSectorsRead * 512,
			WriteBytes: afterSectorsWritten * 512,
			ReadCount:  afterReads,
			WriteCount: afterWrites,
			ReadTime:   afterReadMs,
			WriteTime:  afterWriteMs,
			IoTime:     afterIoMs,
		},
		elapsed,
	)
}
