package ingest

import (
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestHeartbeatSuccess(t *testing.T) {
	var method, auth, userAgent string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		method = r.Method
		auth = r.Header.Get("Authorization")
		userAgent = r.Header.Get("User-Agent")
		body, _ := io.ReadAll(r.Body)
		if len(body) != 0 {
			t.Fatalf("unexpected body: %q", body)
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	cfg := &Config{
		ApiEndpoint: srv.URL + "/ingest",
		IngestToken: "secret",
	}
	if err := Heartbeat(cfg, "2.0.0"); err != nil {
		t.Fatalf("Heartbeat: %v", err)
	}
	if method != http.MethodPost {
		t.Fatalf("method: %q", method)
	}
	if auth != "Bearer secret" {
		t.Fatalf("auth: %q", auth)
	}
	if userAgent != "monitor-agent/2.0.0" {
		t.Fatalf("user agent: %q", userAgent)
	}
}

func TestHeartbeatStatusError(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("invalid token"))
	}))
	defer srv.Close()

	cfg := &Config{
		ApiEndpoint: srv.URL + "/ingest",
		IngestToken: "bad",
	}
	err := Heartbeat(cfg, "")
	if err == nil {
		t.Fatal("expected error")
	}
	if got := err.Error(); got != "heartbeat: status 401: invalid token" {
		t.Fatalf("error: %q", got)
	}
}
