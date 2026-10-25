//go:build linux

package platform

import "testing"

func TestApplyContainerLimits(t *testing.T) {
	got := applyContainerLimits(Options{
		HasZFS: true, EnableGPU: true, EnableBattery: true, EnableThermal: true,
		EnableSmart: true, EnableTimesync: true, EnableDocker: true, EnableTCP: true,
	})
	if got.HasZFS || got.EnableGPU || got.EnableBattery || got.EnableThermal || got.EnableSmart || got.EnableTimesync {
		t.Fatalf("host-level collectors still enabled: %+v", got)
	}
	if !got.EnableDocker || !got.EnableTCP {
		t.Fatalf("unrelated collectors were disabled: %+v", got)
	}
}
