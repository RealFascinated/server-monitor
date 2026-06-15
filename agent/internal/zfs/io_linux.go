//go:build linux

package zfs

import (
	"bufio"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

func ReadPoolIOSnapshots() map[string]PoolIO {
	snapshots := make(map[string]PoolIO)

	matches, err := filepath.Glob("/proc/spl/kstat/zfs/*/iostats")
	if err == nil {
		for _, path := range matches {
			pool := filepath.Base(filepath.Dir(path))
			stats, err := readKstatStats(path)
			if err != nil {
				continue
			}
			snapshots[pool] = poolIOFromKstat(stats)
		}
	}
	if len(snapshots) > 0 {
		return snapshots
	}

	matches, err = filepath.Glob("/proc/spl/kstat/zpool/*/io")
	if err != nil {
		return snapshots
	}
	for _, path := range matches {
		pool := filepath.Base(filepath.Dir(path))
		io, err := parseLegacyPoolIO(path)
		if err != nil {
			continue
		}
		snapshots[pool] = io
	}
	return snapshots
}

func readKstatStats(path string) (map[string]uint64, error) {
	file, err := os.Open(path)
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

func poolIOFromKstat(stats map[string]uint64) PoolIO {
	return PoolIO{
		Nread:    stats["arc_read_bytes"] + stats["direct_read_bytes"],
		Nwritten: stats["arc_write_bytes"] + stats["direct_write_bytes"],
		Reads:    stats["arc_read_count"] + stats["direct_read_count"],
		Writes:   stats["arc_write_count"] + stats["direct_write_count"],
	}
}

func parseLegacyPoolIO(path string) (PoolIO, error) {
	file, err := os.Open(path)
	if err != nil {
		return PoolIO{}, err
	}
	defer file.Close()

	var io PoolIO
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 2 {
			continue
		}
		switch fields[0] {
		case "nread":
			io.Nread = parseUint64(fields[len(fields)-1])
		case "nwritten":
			io.Nwritten = parseUint64(fields[len(fields)-1])
		case "reads":
			io.Reads = parseUint64(fields[len(fields)-1])
		case "writes":
			io.Writes = parseUint64(fields[len(fields)-1])
		}
	}
	return io, scanner.Err()
}

func parseUint64(value string) uint64 {
	n, _ := strconv.ParseUint(value, 10, 64)
	return n
}
