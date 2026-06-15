//go:build linux

package oom

import (
	"bufio"
	"io"
	"os"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

func read() (uint64, bool) {
	if linux.LxcfsActive() || linux.IsContainer() {
		if total, ok := linux.ReadCgroupOOMKills(); ok {
			return total, true
		}
	}
	return readVmstatOOMKills()
}

func readVmstatOOMKills() (uint64, bool) {
	f, err := os.Open(linux.HostPath("/proc/vmstat"))
	if err != nil {
		return 0, false
	}
	defer f.Close()
	return parseVmstatOOMKills(f)
}

func parseVmstatOOMKills(r io.Reader) (uint64, bool) {
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) != 2 || fields[0] != "oom_kill" {
			continue
		}
		total, err := strconv.ParseUint(fields[1], 10, 64)
		if err != nil {
			return 0, false
		}
		return total, true
	}
	return 0, false
}
