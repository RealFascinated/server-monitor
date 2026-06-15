package ingest

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestShouldRetry(t *testing.T) {
	if !shouldRetry(fmt.Errorf("network error")) {
		t.Fatal("expected retry on generic error")
	}
	if !shouldRetry(&httpStatusError{op: "push ingest data", status: http.StatusServiceUnavailable}) {
		t.Fatal("expected retry on 5xx")
	}
	if shouldRetry(&httpStatusError{op: "push ingest data", status: http.StatusBadRequest}) {
		t.Fatal("expected no retry on 4xx")
	}
}

func TestHTTPStatusError(t *testing.T) {
	err := &httpStatusError{op: "push ingest data", status: 500}
	if got := err.Error(); got != "push ingest data: status 500" {
		t.Fatalf("unexpected error: %q", got)
	}

	err = &httpStatusError{op: "push ingest data", status: 400, body: []byte("bad request")}
	if got := err.Error(); got != "push ingest data: status 400: bad request" {
		t.Fatalf("unexpected error: %q", got)
	}
}

func TestPushOnceSuccess(t *testing.T) {
	var auth, userAgent, contentType string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		auth = r.Header.Get("Authorization")
		userAgent = r.Header.Get("User-Agent")
		contentType = r.Header.Get("Content-Type")
		body, _ := io.ReadAll(r.Body)
		if string(body) != `{"ok":true}` {
			t.Fatalf("unexpected body: %s", body)
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}
	if err := post("push ingest data", cfg.ApiEndpoint, cfg.IngestToken, "2.0.0", []byte(`{"ok":true}`)); err != nil {
		t.Fatalf("post: %v", err)
	}
	if auth != "Bearer secret" {
		t.Fatalf("auth header: %q", auth)
	}
	if userAgent != "monitor-agent/2.0.0" {
		t.Fatalf("user agent: %q", userAgent)
	}
	if contentType != "application/json" {
		t.Fatalf("content type: %q", contentType)
	}
}

func TestPushOnceStatusError(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("server busy"))
	}))
	defer srv.Close()

	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}
	err := post("push ingest data", cfg.ApiEndpoint, cfg.IngestToken, "", []byte(`{}`))
	if err == nil {
		t.Fatal("expected error")
	}
	statusErr, ok := err.(*httpStatusError)
	if !ok {
		t.Fatalf("expected httpStatusError, got %T: %v", err, err)
	}
	if statusErr.status != http.StatusInternalServerError {
		t.Fatalf("status: got %d", statusErr.status)
	}
	if !strings.Contains(err.Error(), "server busy") {
		t.Fatalf("error: %v", err)
	}
}

func TestPushSuccess(t *testing.T) {
	attempts := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		if attempts == 1 {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}
	if err := Push(cfg, Data{}, "1.0"); err != nil {
		t.Fatalf("Push: %v", err)
	}
	if attempts != 2 {
		t.Fatalf("expected 2 attempts, got %d", attempts)
	}
}

func TestPushNoRetryOnClientError(t *testing.T) {
	attempts := 0
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		attempts++
		w.WriteHeader(http.StatusBadRequest)
	}))
	defer srv.Close()

	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}
	err := Push(cfg, Data{}, "")
	if err == nil {
		t.Fatal("expected error")
	}
	if attempts != 1 {
		t.Fatalf("expected 1 attempt, got %d", attempts)
	}
}
