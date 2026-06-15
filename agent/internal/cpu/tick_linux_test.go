//go:build linux

package cpu

import (
	"testing"
	"time"

	"fascinated.cc/monitor/agent/internal/linux"
)

func TestComputeLinuxTickOmitsPerCoreWithCgroupCPU(t *testing.T) {
	t.Parallel()

	before := map[string]linux.CPUStat{
		"0": {User: 100, System: 50, Idle: 850},
	}
	after := map[string]linux.CPUStat{
		"0": {User: 200, System: 100, Idle: 900},
	}

	metrics := ComputeLinuxTick(LinuxTickInput{
		PrevProc: linux.ProcStatSnapshot{
			CPU:    before["0"],
			PerCPU: before,
			HasCPU: true,
		},
		CurrProc: linux.ProcStatSnapshot{
			CPU:    after["0"],
			PerCPU: after,
			HasCPU: true,
		},
		HasCgroupCPU: true,
		Elapsed:      time.Second,
	})
	if metrics.CPUCoreMetrics != nil {
		t.Fatalf("CPUCoreMetrics = %#v, want nil when cgroup CPU is used", metrics.CPUCoreMetrics)
	}
}

func TestComputeLinuxTickPerCoreWithoutCgroupCPU(t *testing.T) {
	t.Parallel()

	before := map[string]linux.CPUStat{
		"0": {User: 100, System: 50, Idle: 850},
	}
	after := map[string]linux.CPUStat{
		"0": {User: 200, System: 100, Idle: 900},
	}

	metrics := ComputeLinuxTick(LinuxTickInput{
		PrevProc: linux.ProcStatSnapshot{
			CPU:    before["0"],
			PerCPU: before,
			HasCPU: true,
		},
		CurrProc: linux.ProcStatSnapshot{
			CPU:    after["0"],
			PerCPU: after,
			HasCPU: true,
		},
		Elapsed: time.Second,
	})
	if len(metrics.CPUCoreMetrics) != 1 {
		t.Fatalf("CPUCoreMetrics = %#v, want one core", metrics.CPUCoreMetrics)
	}
}
