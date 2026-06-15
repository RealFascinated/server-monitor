//go:build linux

package cpu

import (
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

func currentClockSpeedMHz() (float64, error) {
	paths, err := filepath.Glob("/sys/devices/system/cpu/cpu[0-9]*/cpufreq/scaling_cur_freq")
	if err != nil {
		return 0, err
	}

	var total uint64
	var count int
	for _, path := range paths {
		data, err := os.ReadFile(path)
		if err != nil {
			continue
		}
		khz, err := strconv.ParseUint(strings.TrimSpace(string(data)), 10, 64)
		if err != nil {
			continue
		}
		total += khz
		count++
	}
	if count > 0 {
		return float64(total) / float64(count) / 1000.0, nil
	}

	return cpuInfoMHzFromProc()
}

func cpuInfoMHzFromProc() (float64, error) {
	data, err := os.ReadFile("/proc/cpuinfo")
	if err != nil {
		return 0, fmt.Errorf("cpu frequency unavailable: %w", err)
	}

	var total float64
	var count int
	for line := range strings.SplitSeq(string(data), "\n") {
		if !strings.HasPrefix(line, "cpu MHz") {
			continue
		}
		parts := strings.SplitN(line, ":", 2)
		if len(parts) != 2 {
			continue
		}
		mhz, err := strconv.ParseFloat(strings.TrimSpace(parts[1]), 64)
		if err != nil || mhz <= 0 {
			continue
		}
		total += mhz
		count++
	}
	if count == 0 {
		return 0, fmt.Errorf("cpu frequency unavailable")
	}
	return total / float64(count), nil
}
