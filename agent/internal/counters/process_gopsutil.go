//go:build !linux

package counters

import (
	"github.com/shirou/gopsutil/v4/load"
	"github.com/shirou/gopsutil/v4/process"
)

func ProcessStats() (total int64, running int64) {
	if misc, err := load.Misc(); err == nil {
		if misc.ProcsTotal > 0 {
			total = int64(misc.ProcsTotal)
		}
		if misc.ProcsRunning > 0 {
			running = int64(misc.ProcsRunning)
		}
	}

	if total == 0 {
		if pids, err := process.Pids(); err == nil {
			total = int64(len(pids))
		}
	}

	if running == 0 {
		running = platformRunningProcesses(total)
	}

	return total, running
}
