//go:build linux

package reboot

import (
	"os"
	"path/filepath"
	"testing"
)

func TestRequiredHonoursHostRoot(t *testing.T) {
	root := t.TempDir()
	t.Setenv("MONITOR_HOST_ROOT", root)

	if Required() {
		t.Fatal("reported a reboot without a flag file")
	}

	if err := os.MkdirAll(filepath.Join(root, "run"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "run", "reboot-required"), nil, 0o644); err != nil {
		t.Fatal(err)
	}
	if !Required() {
		t.Fatal("did not report the host's reboot flag")
	}
}
