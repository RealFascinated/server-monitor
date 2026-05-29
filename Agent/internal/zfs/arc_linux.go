//go:build linux

package zfs

import (
	"bufio"
	"os"
	"strconv"
	"strings"
)

func readArcStats() (map[string]uint64, error) {
	file, err := os.Open(arcStatsPath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	stats := make(map[string]uint64)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 3 {
			continue
		}
		value, err := strconv.ParseUint(fields[2], 10, 64)
		if err != nil {
			continue
		}
		stats[fields[0]] = value
	}

	if err := scanner.Err(); err != nil {
		return nil, err
	}
	if len(stats) == 0 {
		return nil, os.ErrNotExist
	}
	return stats, nil
}
