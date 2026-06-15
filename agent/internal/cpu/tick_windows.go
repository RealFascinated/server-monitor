//go:build windows

package cpu

import (
	"time"

	gcpu "github.com/shirou/gopsutil/v4/cpu"

	"fascinated.cc/monitor/agent/internal/counters"
	"fascinated.cc/monitor/agent/internal/delta"
	"fascinated.cc/monitor/agent/internal/ingest"
	"fascinated.cc/monitor/agent/internal/iostats"
)

type WindowsTickInput struct {
	PrevCPU, CurrCPU           gcpu.TimesStat
	PrevPerCPU, CurrPerCPU     []gcpu.TimesStat
	PrevCounters, CurrCounters counters.SystemCounters
	CountersPerSec             bool
	Elapsed                    time.Duration
}

func ComputeWindowsTick(in WindowsTickInput) ingest.ServerMetrics {
	iowait := EndIowaitSample()
	BeginIowaitSample()

	breakdown := ComputeCPUMetrics(in.PrevCPU, in.CurrCPU)
	metrics := ingest.ServerMetrics{
		CPUUsage:         breakdown.Total,
		CPUUserPercent:   breakdown.User,
		CPUSystemPercent: breakdown.System,
		CPUIowaitPercent: iowait,
		CPUStealPercent:  breakdown.Steal,
	}
	metrics.CPUCoreMetrics = coreMetricsFromUsage(ComputePerCoreCPUMetrics(in.PrevPerCPU, in.CurrPerCPU))
	if watts, ok := EndCPUPowerSample(); ok {
		metrics.CPUPowerWatts = watts
	}
	BeginCPUPowerSample()

	metrics.ProcessCount, metrics.RunningProcesses = counters.ProcessStats()

	if in.CountersPerSec {
		metrics.ContextSwitchesPerSecond = int64(in.PrevCounters.ContextSwitches)
		metrics.InterruptsPerSecond = int64(in.PrevCounters.Interrupts)
	} else {
		metrics.ContextSwitchesPerSecond = iostats.PerSecond(delta.Uint64(in.CurrCounters.ContextSwitches, in.PrevCounters.ContextSwitches), in.Elapsed)
		metrics.InterruptsPerSecond = iostats.PerSecond(delta.Uint64(in.CurrCounters.Interrupts, in.PrevCounters.Interrupts), in.Elapsed)
	}

	return metrics
}

func coreMetricsFromUsage(cores []CoreUsage) []ingest.CPUCoreMetric {
	if len(cores) == 0 {
		return nil
	}
	out := make([]ingest.CPUCoreMetric, len(cores))
	for i, core := range cores {
		out[i] = ingest.CPUCoreMetric{
			CPU:          core.CPU,
			UsagePercent: core.UsagePercent,
		}
	}
	return out
}
