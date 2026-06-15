//go:build windows

package loadavg

import "github.com/shirou/gopsutil/v4/load"

func read() Averages {
	avg, err := load.Avg()
	if err != nil {
		return Averages{}
	}
	return Averages{Load1: avg.Load1, Load5: avg.Load5, Load15: avg.Load15}
}
