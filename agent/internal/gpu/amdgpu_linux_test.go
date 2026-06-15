//go:build linux

package gpu

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadAmdgpuIDs(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "amdgpu.ids")
	content := "" +
		"# comment\n" +
		"744C, C8, AMD Radeon RX 7900 XTX\n" +
		"744C, CC, AMD Radeon RX 7900 XT\n" +
		"15BF, 01, AMD Radeon 760M Graphics\n"
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}

	names := loadAmdgpuIDs(path)
	if got := lookupAmdgpuName("744c", "c8", names); got != "AMD Radeon RX 7900 XTX" {
		t.Fatalf("exact match: %q", got)
	}
	if got := lookupAmdgpuName("744c", "", names); got != "AMD Radeon RX 7900 XTX" {
		t.Fatalf("device fallback: %q", got)
	}
	if got := lookupAmdgpuName("15bf", "01", names); got != "AMD Radeon 760M" {
		t.Fatalf("graphics suffix: %q", got)
	}
}
