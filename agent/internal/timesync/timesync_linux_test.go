//go:build linux

package timesync

import (
	"errors"
	"math"
	"testing"
)

func kernel(status int32, esterrorUs int64) kernelClockReader {
	return func() (kernelClock, bool) { return kernelClock{status: status, esterrorUs: esterrorUs}, true }
}

func noCommands(string, ...string) ([]byte, error) { return nil, errors.New("not installed") }

func TestReadLinuxSynced(t *testing.T) {
	status := readLinux(kernel(0, 2500), noCommands)

	if status.Synced == nil || !*status.Synced {
		t.Fatalf("synced = %v, want true", status.Synced)
	}
	if status.ErrorMs == nil || *status.ErrorMs != 2.5 {
		t.Fatalf("error = %v, want 2.5ms", status.ErrorMs)
	}
	if status.OffsetMs != nil {
		t.Fatalf("offset = %v, want none without a daemon to ask", status.OffsetMs)
	}
}

func TestReadLinuxUnsyncedReportsNoErrorEstimate(t *testing.T) {
	status := readLinux(kernel(staUnsync, 16_000_000), noCommands)

	if status.Synced == nil || *status.Synced {
		t.Fatalf("synced = %v, want false", status.Synced)
	}
	if status.ErrorMs != nil {
		t.Fatalf("error = %v, want none while unsynchronised (the kernel reports its 16s default)", status.ErrorMs)
	}
}

func TestReadLinuxUsesChronyOffset(t *testing.T) {
	run := func(name string, args ...string) ([]byte, error) {
		if name != "chronyc" {
			t.Errorf("unexpected command %q", name)
		}
		return []byte("A29FC87B,1.2.3.4,3,1700000000.0,-0.004000000,0,0,0,0,0,0,0,64,Normal"), nil
	}
	status := readLinux(kernel(0, 100), run)

	if status.OffsetMs == nil || math.Abs(*status.OffsetMs-4) > 1e-9 {
		t.Fatalf("offset = %v, want 4ms", status.OffsetMs)
	}
}

func TestReadLinuxWithoutKernelAccessReportsNothing(t *testing.T) {
	status := readLinux(func() (kernelClock, bool) { return kernelClock{}, false }, noCommands)
	if status.Synced != nil || status.ErrorMs != nil || status.OffsetMs != nil {
		t.Fatalf("status = %+v, want empty", status)
	}
}

func TestReadKernelClockWorksOnThisHost(t *testing.T) {
	if _, ok := readKernelClock(); !ok {
		t.Skip("adjtimex is not permitted in this environment")
	}
}
