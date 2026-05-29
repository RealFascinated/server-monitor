//go:build windows

package counters

import "github.com/yusufpapurcu/wmi"

type perfOSContextSwitches struct {
	ContextSwitchesPersec uint32
}

type perfOSProcesses struct {
	Processes uint32
}

type perfProcessorInterrupts struct {
	InterruptsPersec uint32
}

func ReadSystemCounters() (SystemCounters, bool, error) {
	var counters SystemCounters

	var systemStats []perfOSContextSwitches
	if err := wmi.Query("SELECT ContextSwitchesPersec FROM Win32_PerfFormattedData_PerfOS_System", &systemStats); err == nil && len(systemStats) > 0 {
		counters.ContextSwitches = uint64(systemStats[0].ContextSwitchesPersec)
	}

	var processorStats []perfProcessorInterrupts
	if err := wmi.Query(
		"SELECT InterruptsPersec FROM Win32_PerfFormattedData_Counters_ProcessorInformation WHERE Name='_Total'",
		&processorStats,
	); err == nil && len(processorStats) > 0 {
		counters.Interrupts = uint64(processorStats[0].InterruptsPersec)
	}

	return counters, true, nil
}

func platformRunningProcesses(total int64) int64 {
	var systemStats []perfOSProcesses
	if err := wmi.Query("SELECT Processes FROM Win32_PerfFormattedData_PerfOS_System", &systemStats); err == nil && len(systemStats) > 0 {
		return int64(systemStats[0].Processes)
	}
	return total
}
