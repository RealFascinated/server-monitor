//go:build linux

package battery

import (
	"os"
	"testing"
)

func TestReadCapacityPercent_prefersCapacityFile(t *testing.T) {
	dir := t.TempDir()
	if err := writeFile(dir, "capacity", "73\n"); err != nil {
		t.Fatal(err)
	}

	pct, ok := readCapacityPercent(dir)
	if !ok {
		t.Fatal("expected capacity to be readable")
	}
	if pct != 73 {
		t.Fatalf("got %v, want 73", pct)
	}
}

func TestReadCapacityPercent_fallsBackToEnergy(t *testing.T) {
	dir := t.TempDir()
	if err := writeFile(dir, "energy_now", "25000\n"); err != nil {
		t.Fatal(err)
	}
	if err := writeFile(dir, "energy_full", "50000\n"); err != nil {
		t.Fatal(err)
	}

	pct, ok := readCapacityPercent(dir)
	if !ok {
		t.Fatal("expected energy ratio to be readable")
	}
	if pct != 50 {
		t.Fatalf("got %v, want 50", pct)
	}
}

func writeFile(dir, name, contents string) error {
	return os.WriteFile(dir+"/"+name, []byte(contents), 0o644)
}
