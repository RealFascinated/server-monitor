package zfs

import (
	"testing"
	"time"
)

func TestComputeArcMetrics(t *testing.T) {
	before := ArcSnapshot{Hits: 100, Misses: 100}
	after := ArcSnapshot{
		Size: 1024, Target: 2048, Min: 512, Max: 4096,
		Data: 800, Metadata: 200, L2: 64,
		Hits: 175, Misses: 125,
	}

	metrics := ComputeArcMetrics(before, after, time.Second)
	if metrics == nil {
		t.Fatal("expected metrics")
	}
	if metrics.ArcSizeBytes != 1024 {
		t.Fatalf("size: got %d", metrics.ArcSizeBytes)
	}
	if metrics.ArcHitRatio != 75 {
		t.Fatalf("hit ratio: got %v, want 75", metrics.ArcHitRatio)
	}
	if metrics.ArcMissesPerSecond != 25 {
		t.Fatalf("misses/s: got %d, want 25", metrics.ArcMissesPerSecond)
	}
}

func TestComputeArcMetricsNoHitsOrMisses(t *testing.T) {
	metrics := ComputeArcMetrics(ArcSnapshot{}, ArcSnapshot{}, time.Second)
	if metrics.ArcHitRatio != 0 {
		t.Fatalf("hit ratio: got %v", metrics.ArcHitRatio)
	}
}

func TestComputeArcMetricsCounterReset(t *testing.T) {
	before := ArcSnapshot{Hits: 1000, Misses: 500}
	after := ArcSnapshot{Hits: 10, Misses: 5}

	metrics := ComputeArcMetrics(before, after, 2*time.Second)
	wantRatio := 10.0 / 15.0 * 100
	if diff := metrics.ArcHitRatio - wantRatio; diff > 0.0001 || diff < -0.0001 {
		t.Fatalf("hit ratio: got %v, want %v", metrics.ArcHitRatio, wantRatio)
	}
	if metrics.ArcMissesPerSecond != 2 {
		t.Fatalf("misses/s: got %d, want 2", metrics.ArcMissesPerSecond)
	}
}
