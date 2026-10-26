//go:build linux

package psi

import (
	"os"
	"path/filepath"
	"testing"
)

const cpuPressureFile = `some avg10=0.95 avg60=0.82 avg300=1.29 total=3533508392
full avg10=0.00 avg60=0.00 avg300=0.00 total=0
`

const memoryPressureFile = `some avg10=1.23 avg60=4.56 avg300=7.89 total=600288327
full avg10=0.11 avg60=0.22 avg300=0.33 total=561965336
`

func TestParsePressure(t *testing.T) {
	parsed := parsePressure(memoryPressureFile)

	if !parsed.Some.Available || !parsed.Full.Available {
		t.Fatalf("parsePressure() = %+v, want both stall types available", parsed)
	}
	if parsed.Some.Avg10Pct != 1.23 || parsed.Some.Avg60Pct != 4.56 || parsed.Some.Avg300Pct != 7.89 {
		t.Errorf("some averages = %v %v %v, want 1.23 4.56 7.89",
			parsed.Some.Avg10Pct, parsed.Some.Avg60Pct, parsed.Some.Avg300Pct)
	}
	if parsed.Some.TotalMs != 600288.327 {
		t.Errorf("some total = %v ms, want 600288.327", parsed.Some.TotalMs)
	}
	if parsed.Full.TotalMs != 561965.336 {
		t.Errorf("full total = %v ms, want 561965.336", parsed.Full.TotalMs)
	}
}

func TestParsePressureWithoutFullLine(t *testing.T) {
	parsed := parsePressure(cpuPressureFile[:len("some avg10=0.95 avg60=0.82 avg300=1.29 total=3533508392\n")])

	if !parsed.Some.Available {
		t.Fatal("some should be available")
	}
	if parsed.Full.Available {
		t.Fatalf("full should be absent on kernels that do not report it: %+v", parsed)
	}
	if got := parsed.Full.avg10(); got != nil {
		t.Errorf("Full.avg10() = %v, want nil", *got)
	}
}

func TestParsePressureIgnoresUnusableLines(t *testing.T) {
	for _, data := range []string{"", "not a pressure file\n", "some avg10=1.0 avg60=2.0\n"} {
		if parsed := parsePressure(data); parsed.available() {
			t.Errorf("parsePressure(%q) = %+v, want nothing available", data, parsed)
		}
	}
}

func TestReadReadsHostPressureFiles(t *testing.T) {
	root := t.TempDir()
	writePressureFile(t, root, "cpu", cpuPressureFile)
	writePressureFile(t, root, "memory", memoryPressureFile)

	t.Setenv("MONITOR_HOST_ROOT", root)
	metrics := read()
	if metrics == nil {
		t.Fatal("read() = nil, want metrics")
	}
	if got := deref(t, metrics.CPUSomeAvg10Pct); got != 0.95 {
		t.Errorf("CPUSomeAvg10Pct = %v, want 0.95", got)
	}
	if got := deref(t, metrics.CPUSomeTotalMs); got != 3533508.392 {
		t.Errorf("CPUSomeTotalMs = %v, want 3533508.392", got)
	}
	if got := deref(t, metrics.MemoryFullAvg300Pct); got != 0.33 {
		t.Errorf("MemoryFullAvg300Pct = %v, want 0.33", got)
	}
	if metrics.IOSomeAvg10Pct != nil {
		t.Errorf("IOSomeAvg10Pct = %v, want nil for a missing resource", *metrics.IOSomeAvg10Pct)
	}
}

func TestReadWithoutPSIReturnsNil(t *testing.T) {
	t.Setenv("MONITOR_HOST_ROOT", t.TempDir())
	if metrics := read(); metrics != nil {
		t.Fatalf("read() = %+v, want nil when /proc/pressure is absent", metrics)
	}
}

func writePressureFile(t *testing.T, root, name, data string) {
	t.Helper()
	dir := filepath.Join(root, "proc", "pressure")
	if err := os.MkdirAll(dir, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(dir, name), []byte(data), 0o644); err != nil {
		t.Fatal(err)
	}
}

func deref(t *testing.T, value *float64) float64 {
	t.Helper()
	if value == nil {
		t.Fatal("value is nil, want a value")
	}
	return *value
}
