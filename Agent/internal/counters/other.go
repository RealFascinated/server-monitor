//go:build !linux && !windows

package counters

func ReadSystemCounters() (SystemCounters, bool, error) {
	return SystemCounters{}, false, nil
}

func platformRunningProcesses(total int64) int64 {
	return total
}
