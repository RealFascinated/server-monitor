package fd

import (
	"math"
	"testing"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestApplyToUnlimitedFileMax(t *testing.T) {
	t.Parallel()

	var metrics ingest.ServerMetrics
	ApplyTo(&metrics, Snapshot{Open: 13706, Max: math.MaxInt64})
	if metrics.FdOpen != 13706 {
		t.Fatalf("FdOpen = %d, want 13706", metrics.FdOpen)
	}
	if metrics.FdMax != 0 || metrics.FdUsagePercent != 0 {
		t.Fatalf("expected no max/usage for unlimited, got max=%d usage=%v", metrics.FdMax, metrics.FdUsagePercent)
	}
}

func TestApplyToLimitedFileMax(t *testing.T) {
	t.Parallel()

	var metrics ingest.ServerMetrics
	ApplyTo(&metrics, Snapshot{Open: 1000, Max: 2000})
	if metrics.FdMax != 2000 {
		t.Fatalf("FdMax = %d, want 2000", metrics.FdMax)
	}
	if metrics.FdUsagePercent != 50 {
		t.Fatalf("FdUsagePercent = %v, want 50", metrics.FdUsagePercent)
	}
}
