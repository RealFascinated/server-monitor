//go:build linux

package thermal

import "testing"

func TestNVMeControllerFromPath(t *testing.T) {
	path := "/sys/devices/pci0000:00/0000:05:00.0/nvme/nvme1"
	if got := nvmeControllerFromPath(path); got != "nvme1" {
		t.Fatalf("got %q, want nvme1", got)
	}
	path = "/sys/devices/pci0000:00/0000:06:00.0/nvme/nvme0/nvme0n1"
	if got := nvmeControllerFromPath(path); got != "nvme0" {
		t.Fatalf("namespace path got %q, want nvme0", got)
	}
	if isNVMeController("nvme0n1") {
		t.Fatal("nvme0n1 should not be a controller id")
	}
	if !isNVMeController("nvme10") {
		t.Fatal("nvme10 should be a controller id")
	}
}

func TestIsBlockDeviceName(t *testing.T) {
	if !isBlockDeviceName("nvme0n1") {
		t.Fatal("expected nvme0n1 to be a block device name")
	}
	if isBlockDeviceName("nvme1") {
		t.Fatal("nvme1 controller should not be a block device name")
	}
}
