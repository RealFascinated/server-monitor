package selfstats

import (
	"errors"
	"testing"
	"time"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestRecordPush(t *testing.T) {
	failuresBefore := pushFailuresTotal.Load()

	RecordPush(1500*time.Millisecond, 2048, nil)
	metrics := Collect()
	if metrics.LastPushDurationMs != 1500 || metrics.LastPushPayloadBytes != 2048 {
		t.Fatalf("unexpected last push: %+v", metrics)
	}
	if metrics.PushFailuresTotal != failuresBefore {
		t.Fatalf("success must not count as a failure: %+v", metrics)
	}

	RecordPush(time.Second, 0, errors.New("boom"))
	if got := Collect().PushFailuresTotal; got != failuresBefore+1 {
		t.Fatalf("PushFailuresTotal = %d, want %d", got, failuresBefore+1)
	}
}

func TestCollectReportsRuntimeStats(t *testing.T) {
	metrics := Collect()
	if metrics.Goroutines <= 0 || metrics.HeapInUseBytes <= 0 {
		t.Fatalf("expected runtime stats, got %+v", metrics)
	}
	if metrics.RssBytes <= 0 {
		t.Fatalf("expected RSS > 0, got %d", metrics.RssBytes)
	}
	if metrics.OpenFds <= 0 {
		t.Fatalf("expected open fds > 0, got %d", metrics.OpenFds)
	}
}

func TestAddBufferStats(t *testing.T) {
	metrics := &ingest.AgentMetrics{}
	AddBufferStats(metrics, ingest.QueueStats{Samples: 12, Bytes: 4096, OldestAge: 90 * time.Second, DroppedTotal: 3})

	if metrics.BufferedSamples != 12 || metrics.BufferedBytes != 4096 ||
		metrics.BufferOldestAgeSeconds != 90 || metrics.BufferDroppedTotal != 3 {
		t.Fatalf("unexpected buffer metrics: %+v", metrics)
	}
}
