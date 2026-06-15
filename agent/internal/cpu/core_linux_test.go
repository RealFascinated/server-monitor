//go:build linux

package cpu

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/linux"
)

func TestComputePerCoreCPU(t *testing.T) {
	t.Parallel()

	before := map[string]linux.CPUStat{
		"0": {User: 100, System: 50, Idle: 850},
		"1": {User: 200, System: 100, Idle: 700},
	}
	after := map[string]linux.CPUStat{
		"0": {User: 200, System: 100, Idle: 1000},
		"1": {User: 300, System: 150, Idle: 850},
	}

	got := ComputePerCoreCPU(before, after)
	if len(got) != 2 {
		t.Fatalf("len = %d, want 2", len(got))
	}
	if got[0].CPU != "0" || got[1].CPU != "1" {
		t.Fatalf("cores = %#v", got)
	}
	if got[0].UsagePercent != 50 || got[1].UsagePercent != 50 {
		t.Fatalf("usage = %#v", got)
	}
}
