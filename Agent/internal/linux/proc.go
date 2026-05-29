//go:build linux

package linux

import (
	"bufio"
	"os"
	"regexp"
	"strconv"
	"strings"
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/iostats"
)

type CPUStat struct {
	User, Nice, System, Idle, Iowait, Irq, Softirq, Steal uint64
}

type DiskstatsEntry struct {
	Reads, SectorsRead, ReadMs, Writes, SectorsWritten, WriteMs, IoMs uint64
}

type CgroupIOEntry struct {
	Rbytes, Wbytes uint64
}

type MemoryExtras struct {
	Buffers, Cached, SwapUsed, SwapTotal int64
}

type ProcStatSnapshot struct {
	CPU             CPUStat
	HasCPU          bool
	ContextSwitches uint64
	Interrupts      uint64
}

type LoadavgSnapshot struct {
	Load1, Load5, Load15 float64
	Running, Total       int64
}

type MemorySnapshot struct {
	Usage, Total, Available float64
	Extras                  MemoryExtras
}

var (
	nvmePartPattern = regexp.MustCompile(`^(nvme[0-9]+n[0-9]+)p[0-9]+$`)
	sdPartPattern   = regexp.MustCompile(`^(sd[a-z]+|vd[a-z]+|hd[a-z]+)[0-9]+$`)
)

func ReadProcStat() ProcStatSnapshot {
	file, err := os.Open("/proc/stat")
	if err != nil {
		return ProcStatSnapshot{}
	}
	defer file.Close()

	var snap ProcStatSnapshot
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) == 0 {
			continue
		}
		switch fields[0] {
		case "cpu":
			if len(fields) >= 9 {
				snap.CPU = CPUStat{
					User:    ParseUint64(fields[1]),
					Nice:    ParseUint64(fields[2]),
					System:  ParseUint64(fields[3]),
					Idle:    ParseUint64(fields[4]),
					Iowait:  ParseUint64(fields[5]),
					Irq:     ParseUint64(fields[6]),
					Softirq: ParseUint64(fields[7]),
					Steal:   ParseUint64(fields[8]),
				}
				snap.HasCPU = true
			}
		case "ctxt":
			if len(fields) == 2 {
				snap.ContextSwitches = ParseUint64(fields[1])
			}
		case "intr":
			if len(fields) == 2 {
				snap.Interrupts = ParseUint64(fields[1])
			}
		}
	}
	return snap
}

func ComputeCPUFromProcStat(before, after CPUStat) (usage, user, system, iowait, steal float64) {
	duUser := delta.Uint64(after.User, before.User)
	duNice := delta.Uint64(after.Nice, before.Nice)
	duSys := delta.Uint64(after.System, before.System)
	duIdle := delta.Uint64(after.Idle, before.Idle)
	duIO := delta.Uint64(after.Iowait, before.Iowait)
	duIRQ := delta.Uint64(after.Irq, before.Irq)
	duSoft := delta.Uint64(after.Softirq, before.Softirq)
	duSteal := delta.Uint64(after.Steal, before.Steal)

	total := duUser + duNice + duSys + duIdle + duIO + duIRQ + duSoft + duSteal
	if total == 0 {
		return 0, 0, 0, 0, 0
	}

	usage = float64(total-duIdle) / float64(total) * 100
	user = float64(duUser+duNice) / float64(total) * 100
	system = float64(duSys+duIRQ+duSoft) / float64(total) * 100
	iowait = float64(duIO) / float64(total) * 100
	steal = float64(duSteal) / float64(total) * 100
	return usage, user, system, iowait, steal
}

func ReadLoadavg() LoadavgSnapshot {
	data, err := os.ReadFile("/proc/loadavg")
	if err != nil {
		return LoadavgSnapshot{}
	}
	fields := strings.Fields(string(data))
	if len(fields) < 4 {
		return LoadavgSnapshot{}
	}

	load1, _ := strconv.ParseFloat(fields[0], 64)
	load5, _ := strconv.ParseFloat(fields[1], 64)
	load15, _ := strconv.ParseFloat(fields[2], 64)

	var running, total int64
	if parts := strings.Split(fields[3], "/"); len(parts) == 2 {
		running, _ = strconv.ParseInt(parts[0], 10, 64)
		total, _ = strconv.ParseInt(parts[1], 10, 64)
	}

	return LoadavgSnapshot{
		Load1: load1, Load5: load5, Load15: load15,
		Running: running, Total: total,
	}
}

func ReadMemory() MemorySnapshot {
	if max, current, ok := cgroupMemoryBytes(); ok {
		return MemorySnapshot{
			Usage:     float64(current),
			Total:     float64(max),
			Available: float64(max - current),
		}
	}

	file, err := os.Open("/proc/meminfo")
	if err != nil {
		return MemorySnapshot{}
	}
	defer file.Close()

	var snap MemorySnapshot
	var swapTotal, swapFree uint64
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 2 {
			continue
		}
		valueBytes := int64(ParseUint64(fields[1]) * 1024)
		switch fields[0] {
		case "MemTotal:":
			snap.Total = float64(valueBytes)
		case "MemAvailable:":
			snap.Available = float64(valueBytes)
		case "Buffers:":
			snap.Extras.Buffers = valueBytes
		case "Cached:":
			snap.Extras.Cached = valueBytes
		case "SwapTotal:":
			swapTotal = ParseUint64(fields[1]) * 1024
		case "SwapFree:":
			swapFree = ParseUint64(fields[1]) * 1024
		}
	}
	snap.Usage = snap.Total - snap.Available
	snap.Extras.SwapTotal = int64(swapTotal)
	snap.Extras.SwapUsed = int64(swapTotal - swapFree)
	if snap.Extras.SwapUsed < 0 {
		snap.Extras.SwapUsed = 0
	}
	return snap
}

func ReadDiskstats() map[string]DiskstatsEntry {
	file, err := os.Open("/proc/diskstats")
	if err != nil {
		return map[string]DiskstatsEntry{}
	}
	defer file.Close()

	stats := make(map[string]DiskstatsEntry)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 14 {
			continue
		}
		device := fields[2]
		if strings.HasPrefix(device, "loop") || strings.HasPrefix(device, "ram") ||
			strings.HasPrefix(device, "fd") || strings.HasPrefix(device, "zram") {
			continue
		}
		stats[device] = DiskstatsEntry{
			Reads:          ParseUint64(fields[3]),
			SectorsRead:    ParseUint64(fields[5]),
			ReadMs:         ParseUint64(fields[6]),
			Writes:         ParseUint64(fields[7]),
			SectorsWritten: ParseUint64(fields[9]),
			WriteMs:        ParseUint64(fields[10]),
			IoMs:           ParseUint64(fields[12]),
		}
	}
	return stats
}

func LookupCgroupIOBps(before, after map[string]CgroupIOEntry, elapsed time.Duration) (readBps, writeBps int64) {
	for key, curr := range after {
		prev, ok := before[key]
		if !ok {
			continue
		}
		readBps += iostats.PerSecond(delta.Uint64(curr.Rbytes, prev.Rbytes), elapsed)
		writeBps += iostats.PerSecond(delta.Uint64(curr.Wbytes, prev.Wbytes), elapsed)
	}
	return readBps, writeBps
}

func ParseUint64(value string) uint64 {
	var parsed uint64
	for i := 0; i < len(value); i++ {
		if value[i] < '0' || value[i] > '9' {
			return parsed
		}
		parsed = parsed*10 + uint64(value[i]-'0')
	}
	return parsed
}
