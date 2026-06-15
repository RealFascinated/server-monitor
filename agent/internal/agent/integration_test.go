package agent

import (
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"
)

func TestAgentPushWhenSamplerReady(t *testing.T) {
	var pushes atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Authorization") != "Bearer test-token" {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		pushes.Add(1)
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	config := &Config{
		IngestToken: "test-token",
		ApiEndpoint: srv.URL,
	}

	a := New(config, "2.0.0")
	a.refreshSampler()
	if a.sampler == nil {
		t.Fatal("sampler not initialized")
	}
	if err := a.sampler.Tick(); err != nil {
		t.Fatalf("first tick: %v", err)
	}
	time.Sleep(50 * time.Millisecond)
	if err := a.sampler.Tick(); err != nil {
		t.Fatalf("second tick: %v", err)
	}
	if !a.sampler.Ready() {
		t.Fatal("sampler not ready after two ticks")
	}

	a.pushOnce()

	if pushes.Load() != 1 {
		t.Fatalf("expected 1 push, got %d", pushes.Load())
	}
}
