package ingest

import (
	"bytes"
	"compress/gzip"
	"io"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
)

func resetNegotiation(t *testing.T) {
	t.Helper()
	serverFeatures.Store(nil)
	gzipRejected.Store(false)
	t.Cleanup(func() {
		serverFeatures.Store(nil)
		gzipRejected.Store(false)
	})
}

type recordedRequest struct {
	encoding string
	body     string
}

// ingestServer records each request body (decompressing gzip) and answers with status(encoding).
func ingestServer(t *testing.T, advertise string, status func(encoding string) int) (*httptest.Server, func() []recordedRequest) {
	t.Helper()
	var (
		mu       sync.Mutex
		requests []recordedRequest
	)
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		raw, _ := io.ReadAll(r.Body)
		encoding := r.Header.Get("Content-Encoding")
		decoded := string(raw)
		if encoding == "gzip" {
			reader, err := gzip.NewReader(bytes.NewReader(raw))
			if err != nil {
				t.Errorf("invalid gzip body: %v", err)
			} else if plain, err := io.ReadAll(reader); err == nil {
				decoded = string(plain)
			}
		}
		mu.Lock()
		requests = append(requests, recordedRequest{encoding: encoding, body: decoded})
		mu.Unlock()

		if advertise != "" {
			w.Header().Set(featuresHeader, advertise)
		}
		w.WriteHeader(status(encoding))
	}))
	t.Cleanup(srv.Close)
	return srv, func() []recordedRequest {
		mu.Lock()
		defer mu.Unlock()
		return append([]recordedRequest(nil), requests...)
	}
}

func TestRecordServerFeatures(t *testing.T) {
	resetNegotiation(t)
	if serverSupports(featureGzipIngest) {
		t.Fatal("no features expected before any response")
	}

	header := http.Header{}
	header.Set(featuresHeader, " Gzip-Ingest , batch-ingest ")
	recordServerFeatures(header)
	if !serverSupports(featureGzipIngest) || !serverSupports("batch-ingest") {
		t.Fatal("expected advertised features to be recognised")
	}

	recordServerFeatures(http.Header{})
	if serverSupports(featureGzipIngest) {
		t.Fatal("a response without the header must clear previously known features")
	}
}

func TestPushDoesNotCompressUntilAdvertised(t *testing.T) {
	resetNegotiation(t)
	srv, requests := ingestServer(t, "", func(string) int { return http.StatusOK })

	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}
	if _, err := Push(cfg, Data{AgentVersion: "1.0"}, "1.0"); err != nil {
		t.Fatalf("Push: %v", err)
	}

	got := requests()
	if len(got) != 1 || got[0].encoding != "" {
		t.Fatalf("expected one uncompressed request, got %+v", got)
	}
}

func TestPushCompressesOnceAdvertised(t *testing.T) {
	resetNegotiation(t)
	srv, requests := ingestServer(t, featureGzipIngest, func(string) int { return http.StatusOK })
	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}

	// The first response teaches the agent what the API supports.
	if _, err := Push(cfg, Data{AgentVersion: "1.0"}, "1.0"); err != nil {
		t.Fatalf("first Push: %v", err)
	}
	if _, err := Push(cfg, Data{AgentVersion: "1.0"}, "1.0"); err != nil {
		t.Fatalf("second Push: %v", err)
	}

	got := requests()
	if len(got) != 2 {
		t.Fatalf("expected 2 requests, got %d", len(got))
	}
	if got[0].encoding != "" || got[1].encoding != "gzip" {
		t.Fatalf("expected plain then gzip, got %q then %q", got[0].encoding, got[1].encoding)
	}
	if got[0].body != got[1].body {
		t.Fatalf("decompressed payload differs: %q vs %q", got[0].body, got[1].body)
	}
}

func TestPushFallsBackWhenGzipRejected(t *testing.T) {
	resetNegotiation(t)
	srv, requests := ingestServer(t, featureGzipIngest, func(encoding string) int {
		if encoding == "gzip" {
			return http.StatusUnsupportedMediaType
		}
		return http.StatusOK
	})
	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}

	if _, err := Push(cfg, Data{}, "1.0"); err != nil { // learns the feature
		t.Fatalf("Push: %v", err)
	}
	if _, err := Push(cfg, Data{}, "1.0"); err != nil { // gzip refused, plain accepted
		t.Fatalf("Push with fallback: %v", err)
	}
	if !gzipRejected.Load() {
		t.Fatal("expected compression to be disabled after the fallback succeeded")
	}
	if _, err := Push(cfg, Data{}, "1.0"); err != nil {
		t.Fatalf("Push after fallback: %v", err)
	}

	encodings := []string{}
	for _, request := range requests() {
		encodings = append(encodings, request.encoding)
	}
	want := []string{"", "gzip", "", ""}
	if len(encodings) != len(want) {
		t.Fatalf("requests = %q, want %q", encodings, want)
	}
	for i := range want {
		if encodings[i] != want[i] {
			t.Fatalf("requests = %q, want %q", encodings, want)
		}
	}
}

func TestPushKeepsGzipWhenPayloadIsInvalid(t *testing.T) {
	resetNegotiation(t)
	reject := false
	srv, _ := ingestServer(t, featureGzipIngest, func(string) int {
		if reject {
			return http.StatusBadRequest
		}
		return http.StatusOK
	})
	cfg := &Config{ApiEndpoint: srv.URL, IngestToken: "secret"}

	if _, err := Push(cfg, Data{}, "1.0"); err != nil {
		t.Fatalf("Push: %v", err)
	}
	reject = true
	if _, err := Push(cfg, Data{}, "1.0"); err == nil {
		t.Fatal("expected the 400 to be returned")
	}
	if gzipRejected.Load() {
		t.Fatal("an invalid payload must not disable compression")
	}
}
