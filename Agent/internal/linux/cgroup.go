//go:build linux

package linux

import (
	"bufio"
	"os"
	"runtime"
	"strconv"
	"strings"
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
)

func Dir() string {
	if !IsContainer() {
		return ""
	}

	dir := "/sys/fs/cgroup"
	if _, err := os.Stat(dir + "/cpu.stat"); err == nil {
		return dir
	}
	if _, err := os.Stat(dir + "/memory.current"); err == nil {
		return dir
	}
	return ""
}

func ReadCPUUsageUsec(dir string) (uint64, bool) {
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
			return ParseUint64(fields[1]), true
		}
	}
	return 0, false
}

func cgroupCPUCapacityUsecPerSecond(dir string) uint64 {
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

	quota := ParseUint64(fields[0])
	period := ParseUint64(fields[1])
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

func ComputeCPUUsage(before, after uint64, dir string, elapsed time.Duration) (float64, bool) {
	deltaUse := delta.Uint64(after, before)
	capacity := cgroupCPUCapacityUsecPerSecond(dir)
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

func cgroupMemoryBytes() (max, current uint64, ok bool) {
	dir := Dir()
	if dir == "" {
		return 0, 0, false
	}

	maxData, err := os.ReadFile(dir + "/memory.max")
	if err != nil {
		return 0, 0, false
	}
	currentData, err := os.ReadFile(dir + "/memory.current")
	if err != nil {
		return 0, 0, false
	}

	maxStr := strings.TrimSpace(string(maxData))
	if maxStr == "max" {
		return 0, 0, false
	}

	maxVal, err := strconv.ParseUint(maxStr, 10, 64)
	if err != nil || maxVal == 0 {
		return 0, 0, false
	}
	currentVal, err := strconv.ParseUint(strings.TrimSpace(string(currentData)), 10, 64)
	if err != nil {
		return 0, 0, false
	}
	return maxVal, currentVal, true
}

func ReadIOStats() map[string]CgroupIOEntry {
	dir := Dir()
	if dir == "" {
		return map[string]CgroupIOEntry{}
	}

	file, err := os.Open(dir + "/io.stat")
	if err != nil {
		return map[string]CgroupIOEntry{}
	}
	defer file.Close()

	stats := make(map[string]CgroupIOEntry)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) == 0 {
			continue
		}
		entry := CgroupIOEntry{}
		for _, field := range fields[1:] {
			parts := strings.SplitN(field, "=", 2)
			if len(parts) != 2 {
				continue
			}
			switch parts[0] {
			case "rbytes":
				entry.Rbytes = ParseUint64(parts[1])
			case "wbytes":
				entry.Wbytes = ParseUint64(parts[1])
			}
		}
		stats[fields[0]] = entry
	}
	return stats
}

func ResolveBlockDeviceName(majmin string) string {
	path := "/sys/dev/block/" + majmin
	link, err := os.Readlink(path)
	if err != nil {
		return ""
	}
	if strings.Contains(link, "/") {
		parts := strings.Split(link, "/")
		return parts[len(parts)-1]
	}
	return link
}
