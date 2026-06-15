//go:build linux

package loadavg

func read() Averages {
	snap := readProcLoadavg()
	return Averages{
		Load1:            snap.Load1,
		Load5:            snap.Load5,
		Load15:           snap.Load15,
		RunningProcesses: snap.Running,
		ProcessCount:     snap.Total,
	}
}
