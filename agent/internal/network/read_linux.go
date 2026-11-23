//go:build linux

package network

import (
	"bufio"
	"io"
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
	return parseProcNetDev(file)
}

// parseProcNetDev reads the two header lines and one row per interface. Each row has 16
// columns: receive bytes, packets, errs, drop, ... then transmit bytes, packets, errs, drop.
func parseProcNetDev(r io.Reader) ([]Counter, error) {
	counters := make([]Counter, 0, 8)
	scanner := bufio.NewScanner(r)
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
			Dropin:      parseUint(fields[3]),
			BytesSent:   parseUint(fields[8]),
			PacketsSent: parseUint(fields[9]),
			Errout:      parseUint(fields[10]),
			Dropout:     parseUint(fields[11]),
		})
	}

	return counters, scanner.Err()
}

func parseUint(value string) uint64 {
	n, _ := strconv.ParseUint(value, 10, 64)
	return n
}
