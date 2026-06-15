//go:build !linux

package cpu

import (
	"testing"

	"github.com/shirou/gopsutil/v4/cpu"
)

func TestComputePerCoreCPUMetrics(t *testing.T) {
	t.Parallel()

	before := []cpu.TimesStat{
		{CPU: "cpu0", User: 100, System: 50, Idle: 850},
		{CPU: "cpu1", User: 200, System: 100, Idle: 700},
		{CPU: "cpu-total", User: 300, System: 150, Idle: 1550},
	}
	after := []cpu.TimesStat{
		{CPU: "cpu0", User: 115, System: 60, Idle: 925},
		{CPU: "cpu1", User: 260, System: 70, Idle: 770},
		{CPU: "cpu-total", User: 345, System: 180, Idle: 1575},
	}

	got := ComputePerCoreCPUMetrics(before, after)
	if len(got) != 2 {
		t.Fatalf("len = %d, want 2", len(got))
	}
	if got[0].CPU != "0" || got[1].CPU != "1" {
		t.Fatalf("cores = %#v", got)
	}
	if got[0].UsagePercent != 25 || got[1].UsagePercent != 30 {
		t.Fatalf("usage = %#v", got)
	}
}

func TestPerCoreCPUID(t *testing.T) {
	t.Parallel()

	if id, ok := perCoreCPUID("cpu0"); !ok || id != "0" {
		t.Fatalf("cpu0: id=%q ok=%v", id, ok)
	}
	if _, ok := perCoreCPUID("cpu-total"); ok {
		t.Fatal("expected cpu-total to be skipped")
	}
}
