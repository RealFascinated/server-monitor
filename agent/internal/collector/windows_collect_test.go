//go:build windows

package collector

import (
	"testing"

	cpupkg "fascinated.cc/monitor/agent/internal/cpu"

	"github.com/shirou/gopsutil/v4/cpu"
)

func TestWindowsSamplerCPUBreakdown(t *testing.T) {
	before := cpu.TimesStat{User: 100, System: 50, Idle: 850}
	after := cpu.TimesStat{User: 200, System: 100, Idle: 1600}

	breakdown := cpupkg.ComputeCPUMetrics(before, after)
	if breakdown.Total <= 0 {
		t.Fatalf("expected cpu usage, got %v", breakdown.Total)
	}
}
