package ingest

import "testing"

func TestHashDeviceID(t *testing.T) {
	a := HashDeviceID("GPU-11111111-2222-3333-4444-555555555555")
	b := HashDeviceID("gpu-11111111-2222-3333-4444-555555555555")
	if a != b {
		t.Fatalf("expected case-insensitive hash: %q vs %q", a, b)
	}
	if len(a) != 16 {
		t.Fatalf("expected 16 hex chars, got %q", a)
	}
	if HashDeviceID("") != "" {
		t.Fatal("empty input should return empty")
	}
}
