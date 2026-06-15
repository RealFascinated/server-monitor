package collector

import (
	"testing"
	"time"
)

func TestSamplerClockMHzAfterTick(t *testing.T) {
	s := NewSampler(Options{})
	if err := s.Tick(); err != nil {
		t.Fatalf("first tick: %v", err)
	}
	time.Sleep(10 * time.Millisecond)
	if err := s.Tick(); err != nil {
		t.Fatalf("second tick: %v", err)
	}
	if mhz := s.ClockMHz(); mhz <= 0 {
		t.Fatalf("ClockMHz = %v, want > 0", mhz)
	}
}
