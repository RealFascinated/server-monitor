//go:build linux

package mdraid

import (
	"os"

	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/linux"
)

// Read returns the software RAID arrays from /proc/mdstat.
func Read() []ingest.MdArrayMetric {
	file, err := os.Open(linux.HostPath("/proc/mdstat"))
	if err != nil {
		return nil
	}
	defer file.Close()
	return Parse(file)
}
