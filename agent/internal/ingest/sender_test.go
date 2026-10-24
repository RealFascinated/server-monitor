package ingest

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

type senderRequest struct {
	batch      bool
	timestamps []int64
}

type senderServer struct {
	*httptest.Server
	status   atomic.Int32 // status returned for ingest requests
	batchBad atomic.Int32 // when non-zero, status returned for batch requests only

	mu       sync.Mutex
	requests []senderRequest
}

func newSenderServer(t *testing.T, features string) *senderServer {
	t.Helper()
	s := &senderServer{}
	s.status.Store(http.StatusOK)
	s.Server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		raw, _ := io.ReadAll(r.Body)
		if r.Header.Get("Content-Encoding") == "gzip" {
			raw, _ = gunzip(raw)
		}
		request := senderRequest{batch: strings.HasSuffix(r.URL.Path, "/ingest/batch")}
		if request.batch {
			var batch struct {
				SentAt  int64 `json:"sentAt"`
				Samples []struct {
					Timestamp int64 `json:"timestamp"`
				} `json:"samples"`
			}
			if err := json.Unmarshal(raw, &batch); err != nil || batch.SentAt == 0 {
				t.Errorf("bad batch body (%v): %s", err, raw)
			}
			for _, sample := range batch.Samples {
				request.timestamps = append(request.timestamps, sample.Timestamp)
			}
		} else {
			var sample struct {
				Timestamp int64 `json:"timestamp"`
			}
			_ = json.Unmarshal(raw, &sample)
			request.timestamps = []int64{sample.Timestamp}
		}
		s.mu.Lock()
		s.requests = append(s.requests, request)
		s.mu.Unlock()

		status := int(s.status.Load())
		if bad := int(s.batchBad.Load()); request.batch && bad != 0 {
			status = bad
		}
		if features != "" && status < 400 {
			w.Header().Set(featuresHeader, features)
		}
		w.WriteHeader(status)
	}))
	t.Cleanup(s.Close)
	return s
}

func (s *senderServer) seen() []senderRequest {
	s.mu.Lock()
	defer s.mu.Unlock()
	return append([]senderRequest(nil), s.requests...)
}

func testSender(t *testing.T, srv *senderServer, buffer BufferConfig) (*Sender, *[]time.Duration) {
	t.Helper()
	resetNegotiation(t)
	config := &Config{
		ApiEndpoint: srv.URL + "/v1/servers/ingest",
		IngestToken: "token",
		PushBuffer:  buffer,
	}
	sender := NewSender("1.0", func() *Config { return config })
	waits := &[]time.Duration{}
	sender.wait = func(ctx context.Context, d time.Duration) bool {
		*waits = append(*waits, d)
		return true
	}
	return sender, waits
}

func enabledBuffer() BufferConfig {
	return BufferConfig{Enabled: true, MaxAge: 6 * time.Hour, MaxBytes: 16 << 20}
}

func submit(t *testing.T, s *Sender, timestamps ...int64) {
	t.Helper()
	for _, ts := range timestamps {
		if err := s.Submit(Data{AgentVersion: "1.0", Timestamp: ts}); err != nil {
			t.Fatalf("submit: %v", err)
		}
	}
}

func TestSenderSendsLiveSampleThroughIngestWhenHealthy(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	sender, waits := testSender(t, srv, enabledBuffer())

	submit(t, sender, 100)
	sender.drain(context.Background())

	requests := srv.seen()
	if len(requests) != 1 || requests[0].batch || requests[0].timestamps[0] != 100 {
		t.Fatalf("requests = %+v, want a single live ingest", requests)
	}
	if len(*waits) != 0 || sender.Stats().Samples != 0 {
		t.Fatalf("expected no backoff and an empty queue, waits=%v stats=%+v", *waits, sender.Stats())
	}
}

func TestSenderReplaysBacklogOldestFirstAfterOutage(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	srv.status.Store(http.StatusServiceUnavailable)
	sender, waits := testSender(t, srv, enabledBuffer())
	sender.wait = func(ctx context.Context, d time.Duration) bool {
		*waits = append(*waits, d)
		srv.status.Store(http.StatusOK) // the API comes back while we back off
		return true
	}

	submit(t, sender, 1, 2, 3)
	sender.drain(context.Background())

	requests := srv.seen()
	if len(requests) != 3 {
		t.Fatalf("requests = %+v, want failed batch, batch, live", requests)
	}
	if !requests[1].batch || len(requests[1].timestamps) != 2 || requests[1].timestamps[0] != 1 || requests[1].timestamps[1] != 2 {
		t.Fatalf("backlog = %+v, want batch of [1 2] in order", requests[1])
	}
	if requests[2].batch || requests[2].timestamps[0] != 3 {
		t.Fatalf("newest = %+v, want live ingest of 3", requests[2])
	}
	if len(*waits) != 1 || (*waits)[0] < 1500*time.Millisecond || (*waits)[0] > 2500*time.Millisecond {
		t.Fatalf("backoff = %v, want ~2s", *waits)
	}
	if stats := sender.Stats(); stats.Samples != 0 || stats.DroppedTotal != 0 {
		t.Fatalf("stats = %+v, want everything delivered", stats)
	}
}

func TestSenderBackoffGrowsAndIsCapped(t *testing.T) {
	var d time.Duration
	var seen []time.Duration
	for i := 0; i < 8; i++ {
		d = nextBackoff(d)
		seen = append(seen, d)
	}
	want := []time.Duration{2, 4, 8, 16, 32, 60, 60, 60}
	for i := range want {
		if seen[i] != want[i]*time.Second {
			t.Fatalf("backoff sequence = %v", seen)
		}
	}
}

func TestSenderKeepsSamplesWhileApiStaysDown(t *testing.T) {
	srv := newSenderServer(t, "")
	srv.status.Store(http.StatusServiceUnavailable)
	sender, _ := testSender(t, srv, enabledBuffer())
	ctx, cancel := context.WithCancel(context.Background())
	sender.wait = func(context.Context, time.Duration) bool {
		cancel() // give up after the first backoff, as a shutdown would
		return false
	}

	submit(t, sender, 1, 2, 3)
	sender.drain(ctx)

	if stats := sender.Stats(); stats.Samples != 3 || stats.DroppedTotal != 0 {
		t.Fatalf("stats = %+v, want all 3 samples still buffered", stats)
	}
}

func TestSenderDropsSamplesTheApiRejects(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	srv.status.Store(http.StatusUnauthorized)
	sender, waits := testSender(t, srv, enabledBuffer())

	submit(t, sender, 1)
	sender.drain(context.Background())

	if len(*waits) != 0 {
		t.Fatalf("a 401 must not be retried, waits=%v", *waits)
	}
	if stats := sender.Stats(); stats.Samples != 0 || stats.DroppedTotal != 1 {
		t.Fatalf("stats = %+v, want the sample dropped", stats)
	}
}

func TestSenderDropsRejectedBatchAndStillSendsLiveSample(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	srv.batchBad.Store(http.StatusBadRequest)
	sender, _ := testSender(t, srv, enabledBuffer())

	submit(t, sender, 1, 2, 3)
	sender.drain(context.Background())

	requests := srv.seen()
	last := requests[len(requests)-1]
	if last.batch || last.timestamps[0] != 3 {
		t.Fatalf("requests = %+v, want the live sample to go out after the bad batch", requests)
	}
	if stats := sender.Stats(); stats.Samples != 0 || stats.DroppedTotal != 2 {
		t.Fatalf("stats = %+v, want 2 dropped", stats)
	}
}

func TestSenderRetriesOnRateLimit(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	srv.batchBad.Store(http.StatusTooManyRequests)
	sender, waits := testSender(t, srv, enabledBuffer())
	sender.wait = func(ctx context.Context, d time.Duration) bool {
		*waits = append(*waits, d)
		srv.batchBad.Store(0)
		return true
	}

	submit(t, sender, 1, 2)
	sender.drain(context.Background())

	if len(*waits) != 1 || sender.Stats().DroppedTotal != 0 {
		t.Fatalf("waits=%v stats=%+v, want one backoff and nothing dropped", *waits, sender.Stats())
	}
}

func TestSenderChunksLargeBacklogs(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	sender, _ := testSender(t, srv, enabledBuffer())

	for i := int64(1); i <= 250; i++ {
		submit(t, sender, i)
	}
	sender.drain(context.Background())

	var sizes []int
	var order []int64
	for _, request := range srv.seen() {
		sizes = append(sizes, len(request.timestamps))
		order = append(order, request.timestamps...)
	}
	if len(sizes) != 4 || sizes[0] != 100 || sizes[1] != 100 || sizes[2] != 49 || sizes[3] != 1 {
		t.Fatalf("request sizes = %v, want 100,100,49 batches then 1 live", sizes)
	}
	for i, ts := range order {
		if ts != int64(i+1) {
			t.Fatalf("samples delivered out of order at %d: %v", i, order)
		}
	}
}

func TestSenderSendsStaleSamplesThroughBatchEndpoint(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	sender, _ := testSender(t, srv, enabledBuffer())
	clock := &fakeClock{t: time.Unix(1_700_000_000, 0)}
	sender.queue = newSampleQueue(clock.now)
	sender.now = clock.now

	submit(t, sender, 1)
	clock.advance(5 * time.Minute)
	sender.drain(context.Background())

	requests := srv.seen()
	if len(requests) != 1 || !requests[0].batch {
		t.Fatalf("requests = %+v, want the stale sample backfilled, not sent as live", requests)
	}
}

func TestSenderWithoutBatchSupportSendsOnlyNewestAndDropsFailures(t *testing.T) {
	srv := newSenderServer(t, featureGzipIngest) // an API that predates batch ingest
	sender, _ := testSender(t, srv, enabledBuffer())
	recordServerFeatures(http.Header{featuresHeader: []string{featureGzipIngest}})

	submit(t, sender, 1, 2, 3)
	sender.drain(context.Background())

	requests := srv.seen()
	if len(requests) != 1 || requests[0].batch || requests[0].timestamps[0] != 3 {
		t.Fatalf("requests = %+v, want only the newest sample sent live", requests)
	}

	srv.status.Store(http.StatusBadRequest)
	submit(t, sender, 4)
	sender.drain(context.Background())
	if stats := sender.Stats(); stats.Samples != 0 {
		t.Fatalf("stats = %+v, want the failed sample dropped rather than buffered", stats)
	}
}

func TestSenderWithBufferDisabledKeepsOnlyOneSample(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	sender, _ := testSender(t, srv, BufferConfig{Enabled: false})

	submit(t, sender, 1, 2, 3)
	if stats := sender.Stats(); stats.Samples != 1 {
		t.Fatalf("stats = %+v, want the queue capped at the newest sample", stats)
	}
	sender.drain(context.Background())

	if requests := srv.seen(); len(requests) != 1 || requests[0].timestamps[0] != 3 {
		t.Fatalf("requests = %+v, want only sample 3", requests)
	}
}

func TestSenderRunStopsOnContextCancel(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	sender, _ := testSender(t, srv, enabledBuffer())

	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan struct{})
	go func() {
		sender.Run(ctx)
		close(done)
	}()

	submit(t, sender, 7)
	deadline := time.After(5 * time.Second)
	for len(srv.seen()) == 0 {
		select {
		case <-deadline:
			t.Fatal("sample was never delivered")
		case <-time.After(5 * time.Millisecond):
		}
	}

	cancel()
	select {
	case <-done:
	case <-time.After(5 * time.Second):
		t.Fatal("Run did not return after cancel")
	}
}

func TestSenderObservesEveryAttempt(t *testing.T) {
	srv := newSenderServer(t, featureBatchIngest)
	srv.status.Store(http.StatusServiceUnavailable)
	sender, _ := testSender(t, srv, enabledBuffer())
	sender.wait = func(context.Context, time.Duration) bool {
		srv.status.Store(http.StatusOK)
		return true
	}
	var failures, successes int
	sender.Observe = func(_ time.Duration, payloadBytes int, err error) {
		if payloadBytes <= 0 {
			t.Errorf("payload bytes = %d", payloadBytes)
		}
		if err != nil {
			failures++
		} else {
			successes++
		}
	}

	submit(t, sender, 1)
	sender.drain(context.Background())

	if failures != 1 || successes != 1 {
		t.Fatalf("observed %d failures and %d successes, want 1 and 1", failures, successes)
	}
}
