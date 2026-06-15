//go:build linux

package fd

import (
	"os"
	"strconv"
	"strings"

	"fascinated.cc/monitor/agent/internal/linux"
)

func read() Snapshot {
	data, err := os.ReadFile(linux.HostPath("/proc/sys/fs/file-nr"))
	if err != nil {
		return Snapshot{}
	}
	return parseFileNr(string(data))
}

func parseFileNr(data string) Snapshot {
	fields := strings.Fields(strings.TrimSpace(data))
	if len(fields) < 3 {
		return Snapshot{}
	}
	open, err1 := strconv.ParseInt(fields[0], 10, 64)
	max, err2 := strconv.ParseInt(fields[2], 10, 64)
	if err1 != nil || err2 != nil {
		return Snapshot{}
	}
	return Snapshot{Open: open, Max: max}
}
