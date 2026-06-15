//go:build linux

package gpu

import (
	"os"
	"path/filepath"
	"testing"
)

func TestDrmDeviceIDFromUevent(t *testing.T) {
	dir := t.TempDir()
	uevent := filepath.Join(dir, "uevent")
	if err := os.WriteFile(uevent, []byte("DRIVER=amdgpu\nPCI_SLOT_NAME=0000:03:00.0\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	if id := drmDeviceID(dir); id != "0000:03:00.0" {
		t.Fatalf("device id: %q", id)
	}
}

func TestReadUeventValue(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "uevent")
	if err := os.WriteFile(path, []byte("PCI_SLOT_NAME=0000:0a:00.0\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	if got := readUeventValue(path, "PCI_SLOT_NAME"); got != "0000:0a:00.0" {
		t.Fatalf("got %q", got)
	}
}
