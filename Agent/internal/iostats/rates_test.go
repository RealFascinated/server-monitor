package iostats

import (
	"testing"
	"time"
)

func TestPerSecond(t *testing.T) {
	if got := PerSecond(2000, 2*time.Second); got != 1000 {
		t.Fatalf("expected 1000, got %d", got)
	}
}

func TestRatesFromIO(t *testing.T) {
	before := IOCounts{ReadBytes: 0, WriteBytes: 0, ReadCount: 0, WriteCount: 0}
	after := IOCounts{ReadBytes: 2048, WriteBytes: 1024, ReadCount: 2, WriteCount: 1}
	rates := RatesFromIO(before, after, time.Second)
	if rates.ReadBytesPerSecond != 2048 || rates.WriteBytesPerSecond != 1024 {
		t.Fatalf("unexpected rates: %+v", rates)
	}
}

func TestPerSecondZeroElapsed(t *testing.T) {
	if got := PerSecond(500, 0); got != 500 {
		t.Fatalf("expected 500, got %d", got)
	}
}

func TestPerSecondFloat(t *testing.T) {
	if got := PerSecondFloat(300, 3*time.Second); got != 100 {
		t.Fatalf("expected 100, got %v", got)
	}
	if got := PerSecondFloat(42, 0); got != 42 {
		t.Fatalf("expected 42 with zero elapsed, got %v", got)
	}
}

func TestRatesFromIOCapsUsage(t *testing.T) {
	before := IOCounts{}
	after := IOCounts{IoTime: 20000} // 2000ms of IO time in 1s window -> 200% before cap
	rates := RatesFromIO(before, after, time.Second)
	if rates.IoUsagePercent != 100 {
		t.Fatalf("expected capped usage 100, got %v", rates.IoUsagePercent)
	}
}

func TestRatesFromDiskstats(t *testing.T) {
	rates := RatesFromDiskstats(
		0, 2, 0, 0, 1, 0, 0,
		1, 4, 10, 1, 2, 20, 100,
		time.Second,
	)
	if rates.ReadBytesPerSecond != 1024 {
		t.Fatalf("read B/s: got %d", rates.ReadBytesPerSecond)
	}
	if rates.WriteBytesPerSecond != 512 {
		t.Fatalf("write B/s: got %d", rates.WriteBytesPerSecond)
	}
	if rates.ReadIops != 1 || rates.WriteIops != 1 {
		t.Fatalf("unexpected iops: %+v", rates)
	}
}
