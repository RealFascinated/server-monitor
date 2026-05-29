//go:build linux

package loadavg

import "fascinated.cc/monitor/agent/internal/linux"

func read() Averages {
	snap := linux.ReadLoadavg()
	return Averages{Load1: snap.Load1, Load5: snap.Load5, Load15: snap.Load15}
}
