package collector

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestOverlaySlowServerMetricsCarriesClockStatus(t *testing.T) {
	synced, errMs, offsetMs := true, 1.5, -0.25
	var dest ingest.ServerMetrics

	overlaySlowServerMetrics(&dest, ingest.ServerMetrics{ClockSynced: &synced, ClockErrorMs: &errMs, ClockOffsetMs: &offsetMs})

	if dest.ClockSynced == nil || !*dest.ClockSynced || *dest.ClockErrorMs != 1.5 || *dest.ClockOffsetMs != -0.25 {
		t.Fatalf("clock status not copied: %+v", dest)
	}

	// A platform that stops reporting (daemon removed) must not leave a stale value behind.
	overlaySlowServerMetrics(&dest, ingest.ServerMetrics{})
	if dest.ClockSynced != nil || dest.ClockErrorMs != nil || dest.ClockOffsetMs != nil {
		t.Fatalf("stale clock status kept: %+v", dest)
	}
}
