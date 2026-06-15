//go:build linux

package linux

import (
	"bufio"
	"io"
	"os"
	"regexp"
	"strings"
	"time"

	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/iostats"
)

type DiskstatsEntry struct {
	Reads, SectorsRead, ReadMs, Writes, SectorsWritten, WriteMs, IoMs uint64
}

type CgroupIOEntry struct {
	Rbytes, Wbytes uint64
}

type CPUStat struct {
	User, Nice, System, Idle, Iowait, Irq, Softirq, Steal uint64
}

type ProcStatSnapshot struct {
	CPU             CPUStat
	PerCPU          map[string]CPUStat
	HasCPU          bool
	ContextSwitches uint64
	Interrupts      uint64
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
	return parseProcStat(file)
}

func parseProcStat(r io.Reader) ProcStatSnapshot {
	var snap ProcStatSnapshot
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) == 0 {
			continue
		}
		switch {
		case strings.HasPrefix(fields[0], "cpu") && len(fields) >= 9:
			stat := parseCPUStatFields(fields[1:])
			if fields[0] == "cpu" {
				snap.CPU = stat
				snap.HasCPU = true
			} else if id := strings.TrimPrefix(fields[0], "cpu"); id != "" {
				if snap.PerCPU == nil {
					snap.PerCPU = make(map[string]CPUStat)
				}
				snap.PerCPU[id] = stat
			}
		case fields[0] == "ctxt":
			if len(fields) == 2 {
				snap.ContextSwitches = ParseUint64(fields[1])
			}
		case fields[0] == "intr":
			if len(fields) >= 2 {
				snap.Interrupts = ParseUint64(fields[1])
			}
		}
	}
	return snap
}

func FilterPerCPU(stats map[string]CPUStat, allowed map[string]struct{}) map[string]CPUStat {
	if len(stats) == 0 || len(allowed) == 0 {
		return stats
	}
	out := make(map[string]CPUStat, len(allowed))
	for id, stat := range stats {
		if _, ok := allowed[id]; ok {
			out[id] = stat
		}
	}
	return out
}

func parseCPUStatFields(fields []string) CPUStat {
	if len(fields) < 8 {
		return CPUStat{}
	}
	return CPUStat{
		User:    ParseUint64(fields[0]),
		Nice:    ParseUint64(fields[1]),
		System:  ParseUint64(fields[2]),
		Idle:    ParseUint64(fields[3]),
		Iowait:  ParseUint64(fields[4]),
		Irq:     ParseUint64(fields[5]),
		Softirq: ParseUint64(fields[6]),
		Steal:   ParseUint64(fields[7]),
	}
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
