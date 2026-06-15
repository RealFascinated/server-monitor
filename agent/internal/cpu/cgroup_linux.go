//go:build linux

package cpu

import (
	"os"
	"runtime"
	"strings"
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/linux"
)

func ReadCgroupUsageUsec(dir string) (uint64, bool) {
	if dir == "" {
		return 0, false
	}
	data, err := os.ReadFile(dir + "/cpu.stat")
	if err != nil {
		return 0, false
	}
	for line := range strings.SplitSeq(string(data), "\n") {
		fields := strings.Fields(line)
		if len(fields) == 2 && fields[0] == "usage_usec" {
			return linux.ParseUint64(fields[1]), true
		}
	}
	return 0, false
}

func CgroupUsagePercent(before, after uint64, dir string, elapsed time.Duration) (float64, bool) {
	deltaUse := delta.Uint64(after, before)
	capacity := cgroupCapacityUsecPerSecond(dir)
	if capacity == 0 {
		return 0, true
	}
	seconds := elapsed.Seconds()
	if seconds <= 0 {
		seconds = 1
	}
	usage := float64(deltaUse) / (float64(capacity) * seconds) * 100
	if usage > 100 {
		usage = 100
	}
	return usage, true
}

func cgroupCapacityUsecPerSecond(dir string) uint64 {
	online := uint64(runtime.NumCPU())
	if dir == "" {
		return online * 1_000_000
	}

	data, err := os.ReadFile(dir + "/cpu.max")
	if err != nil {
		return online * 1_000_000
	}
	fields := strings.Fields(string(data))
	if len(fields) != 2 || fields[0] == "max" {
		return online * 1_000_000
	}

	quota := linux.ParseUint64(fields[0])
	period := linux.ParseUint64(fields[1])
	if period == 0 {
		return online * 1_000_000
	}

	capacity := quota * 1_000_000 / period
	maxCapacity := online * 1_000_000
	if capacity > 0 && capacity < maxCapacity {
		return capacity
	}
	return maxCapacity
}
