//go:build !linux

package cpu

import (
	"testing"

	"github.com/shirou/gopsutil/v4/cpu"
)

func stat(user, system, idle, iowait, steal float64) cpu.TimesStat {
	return cpu.TimesStat{
		User:   user,
		System: system,
		Idle:   idle,
		Iowait: iowait,
		Steal:  steal,
	}
}

func TestComputeCPUMetrics(t *testing.T) {
	before := stat(100, 50, 800, 40, 10)
	after := stat(150, 75, 720, 60, 15)

	got := ComputeCPUMetrics(before, after)
	if got.Total <= 0 {
		t.Fatalf("expected positive total usage, got %+v", got)
	}
	if got.User != 100 {
		t.Fatalf("user: got %v, want 100", got.User)
	}
	if got.System != 100 {
		t.Fatalf("system: got %v, want 100", got.System)
	}
	if got.Iowait != 100 {
		t.Fatalf("iowait: got %v, want 100", got.Iowait)
	}
	if got.Steal != 25 {
		t.Fatalf("steal: got %v, want 25", got.Steal)
	}
}

func TestComputeCPUMetricsZeroDelta(t *testing.T) {
	s := stat(100, 50, 800, 40, 10)
	got := ComputeCPUMetrics(s, s)
	if got != (CPUMetrics{}) {
		t.Fatalf("expected empty metrics, got %+v", got)
	}
}

func TestSocketCount(t *testing.T) {
	if got := SocketCount(nil); got != 0 {
		t.Fatalf("expected 0, got %d", got)
	}
	if got := SocketCount([]cpu.InfoStat{{PhysicalID: "0"}, {PhysicalID: "1"}}); got != 2 {
		t.Fatalf("expected 2 sockets, got %d", got)
	}
	if got := SocketCount([]cpu.InfoStat{{ModelName: "cpu"}}); got != 1 {
		t.Fatalf("expected fallback 1, got %d", got)
	}
}
