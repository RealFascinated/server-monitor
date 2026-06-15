//go:build linux

package memory

import (
	"bufio"
	"os"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

func Read() Snapshot {
	snap := readMeminfo()
	return overlayCgroupMemory(snap, linux.ReadCgroupMemory(uint64(snap.Total)))
}

func readMeminfo() Snapshot {
	file, err := os.Open("/proc/meminfo")
	if err != nil {
		return Snapshot{}
	}
	defer file.Close()

	var snap Snapshot
	var swapTotal, swapFree uint64
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 2 {
			continue
		}
		valueBytes := int64(linux.ParseUint64(fields[1]) * 1024)
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
			swapTotal = linux.ParseUint64(fields[1]) * 1024
		case "SwapFree:":
			swapFree = linux.ParseUint64(fields[1]) * 1024
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

func overlayCgroupMemory(snap Snapshot, cg linux.CgroupMemory) Snapshot {
	if !cg.OK {
		return snap
	}
	snap.Total = float64(cg.Max)
	snap.Usage = float64(cg.Usage())
	snap.Available = float64(cg.Available())
	return snap
}
