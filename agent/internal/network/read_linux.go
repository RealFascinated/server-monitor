//go:build linux

package network

import (
	"bufio"
	"os"
	"strconv"
	"strings"
)

func ReadCounters() ([]Counter, error) {
	file, err := os.Open("/proc/net/dev")
	if err != nil {
		return nil, err
	}
	defer file.Close()

	counters := make([]Counter, 0, 8)
	scanner := bufio.NewScanner(file)
	lineNum := 0
	for scanner.Scan() {
		lineNum++
		if lineNum <= 2 {
			continue
		}

		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}

		name, stats, ok := strings.Cut(line, ":")
		if !ok {
			continue
		}
		name = strings.TrimSpace(name)

		fields := strings.Fields(stats)
		if len(fields) < 16 {
			continue
		}

		counters = append(counters, Counter{
			Name:        name,
			BytesRecv:   parseUint(fields[0]),
			PacketsRecv: parseUint(fields[1]),
			Errin:       parseUint(fields[2]),
			BytesSent:   parseUint(fields[8]),
			PacketsSent: parseUint(fields[9]),
			Errout:      parseUint(fields[10]),
		})
	}

	return counters, scanner.Err()
}

func parseUint(value string) uint64 {
	n, _ := strconv.ParseUint(value, 10, 64)
	return n
}
