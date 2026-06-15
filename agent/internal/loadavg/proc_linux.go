//go:build linux

package loadavg

import (
	"os"
	"strconv"
	"strings"
)

type procSnapshot struct {
	Load1, Load5, Load15 float64
	Running, Total       int64
}

func readProcLoadavg() procSnapshot {
	data, err := os.ReadFile("/proc/loadavg")
	if err != nil {
		return procSnapshot{}
	}
	fields := strings.Fields(string(data))
	if len(fields) < 4 {
		return procSnapshot{}
	}

	load1, _ := strconv.ParseFloat(fields[0], 64)
	load5, _ := strconv.ParseFloat(fields[1], 64)
	load15, _ := strconv.ParseFloat(fields[2], 64)

	var running, total int64
	if parts := strings.Split(fields[3], "/"); len(parts) == 2 {
		running, _ = strconv.ParseInt(parts[0], 10, 64)
		total, _ = strconv.ParseInt(parts[1], 10, 64)
	}

	return procSnapshot{
		Load1: load1, Load5: load5, Load15: load15,
		Running: running, Total: total,
	}
}
