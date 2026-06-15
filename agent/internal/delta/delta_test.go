package delta

import "testing"

func TestUint64NormalDelta(t *testing.T) {
	if got := Uint64(100, 40); got != 60 {
		t.Fatalf("expected 60, got %d", got)
	}
}

func TestUint64CounterReset(t *testing.T) {
	if got := Uint64(10, 100); got != 10 {
		t.Fatalf("expected 10 on reset, got %d", got)
	}
}
