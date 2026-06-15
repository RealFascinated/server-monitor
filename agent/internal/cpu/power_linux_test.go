//go:build linux

package cpu

import (
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestComputeCPUPowerWatts(t *testing.T) {
	elapsed := time.Second
	watts, ok := ComputePowerWatts(0, 65_000_000, 0, elapsed)
	if !ok || watts < 64.9 || watts > 65.1 {
		t.Fatalf("watts = %v ok=%v, want ~65", watts, ok)
	}

	watts, ok = ComputePowerWatts(0, 0, 0, elapsed)
	if ok || watts != 0 {
		t.Fatalf("zero delta should fail: watts=%v ok=%v", watts, ok)
	}
}

func TestEnergyDeltaWrap(t *testing.T) {
	const max = 1000
	if got := energyDelta(900, 100, max); got != 200 {
		t.Fatalf("wrap delta = %d, want 200", got)
	}
	if got := energyDelta(100, 200, max); got != 100 {
		t.Fatalf("normal delta = %d, want 100", got)
	}
}

func TestDiscoverIntelRAPLEnergy(t *testing.T) {
	root := t.TempDir()
	rapl := filepath.Join(root, "sys", "class", "powercap", "intel-rapl", "intel-rapl:0")
	if err := os.MkdirAll(rapl, 0o755); err != nil {
		t.Fatal(err)
	}
	writeFile(t, filepath.Join(rapl, "name"), "package-0\n")
	writeFile(t, filepath.Join(rapl, "energy_uj"), "12345\n")
	writeFile(t, filepath.Join(rapl, "max_energy_range_uj"), "99999\n")

	t.Setenv("MONITOR_HOST_ROOT", root)
	src, ok := discoverIntelRAPLEnergy()
	if !ok || len(src.paths) != 1 {
		t.Fatalf("discoverIntelRAPLEnergy() = %+v ok=%v", src, ok)
	}
}

func TestDiscoverAMDEnergy(t *testing.T) {
	root := t.TempDir()
	hwmon := filepath.Join(root, "sys", "class", "hwmon", "hwmon0")
	if err := os.MkdirAll(hwmon, 0o755); err != nil {
		t.Fatal(err)
	}
	writeFile(t, filepath.Join(hwmon, "name"), "amd_energy\n")
	writeFile(t, filepath.Join(hwmon, "energy32_label"), "Esocket0\n")
	writeFile(t, filepath.Join(hwmon, "energy32_input"), "5000\n")

	t.Setenv("MONITOR_HOST_ROOT", root)
	src, ok := discoverAMDEnergy()
	if !ok || len(src.paths) != 1 {
		t.Fatalf("discoverAMDEnergy() = %+v ok=%v", src, ok)
	}
}

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}
