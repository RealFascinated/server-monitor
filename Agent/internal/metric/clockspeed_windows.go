//go:build windows

package metric

import (
	"fmt"

	"github.com/yusufpapurcu/wmi"
)

type processorPerformance struct {
	ActualFrequency             uint32
	PercentProcessorPerformance uint32
}

type win32Processor struct {
	MaxClockSpeed uint32
}

func currentClockSpeedMHz() (float64, error) {
	var perf []processorPerformance
	if err := wmi.Query(
		"SELECT ActualFrequency, PercentProcessorPerformance FROM Win32_PerfFormattedData_Counters_ProcessorInformation WHERE Name='_Total'",
		&perf,
	); err != nil {
		return 0, err
	}
	if len(perf) == 0 {
		return 0, fmt.Errorf("cpu frequency unavailable")
	}
	if perf[0].ActualFrequency > 0 {
		return float64(perf[0].ActualFrequency), nil
	}
	if perf[0].PercentProcessorPerformance == 0 {
		return 0, fmt.Errorf("cpu frequency unavailable")
	}

	var processors []win32Processor
	if err := wmi.Query("SELECT MaxClockSpeed FROM Win32_Processor", &processors); err != nil {
		return 0, err
	}
	if len(processors) == 0 || processors[0].MaxClockSpeed == 0 {
		return 0, fmt.Errorf("cpu frequency unavailable")
	}

	return float64(processors[0].MaxClockSpeed) * float64(perf[0].PercentProcessorPerformance) / 100.0, nil
}
