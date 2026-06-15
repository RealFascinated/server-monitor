//go:build windows

package cpu

import (
	"testing"
	"time"
)

func TestEndCPUPowerSample(t *testing.T) {
	BeginCPUPowerSample()
	time.Sleep(200 * time.Millisecond)

	watts, ok := EndCPUPowerSample()
	if !ok {
		t.Skip("RAPL energy meter counter unavailable on this host")
	}
	if watts <= 0 || watts > 10_000 {
		t.Fatalf("watts = %v ok=%v, want reasonable package power", watts, ok)
	}
}
