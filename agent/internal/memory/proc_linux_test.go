//go:build linux

package memory

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/linux"
)

func TestOverlayCgroupMemoryProxmox(t *testing.T) {
	t.Parallel()

	snap := Snapshot{Total: 8589934592}
	cg := linux.CgroupMemory{
		Max:     8589934592,
		Current: 1069248512,
		File:    811769856,
		OK:      true,
	}

	got := overlayCgroupMemory(snap, cg)
	wantUsage := uint64(257478656)
	if got.Usage != float64(wantUsage) {
		t.Fatalf("usage = %v, want %v", got.Usage, wantUsage)
	}
	if got.Available != float64(8589934592-wantUsage) {
		t.Fatalf("available = %v, want %v", got.Available, 8589934592-wantUsage)
	}
}
