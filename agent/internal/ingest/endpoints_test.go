package ingest

import "testing"

func TestSiblingEndpoint(t *testing.T) {
	tests := []struct {
		apiEndpoint string
		want        string
	}{
		{"", defaultHeartbeatEndpoint},
		{"https://monitor.fascinated.cc/api/v1/servers/ingest", "https://monitor.fascinated.cc/api/v1/servers/heartbeat"},
		{"https://api.example/ingest", "https://api.example/heartbeat"},
		{"https://api.example/custom", "https://api.example/heartbeat"},
	}
	for _, tc := range tests {
		if got := siblingEndpoint(tc.apiEndpoint, "heartbeat", defaultHeartbeatEndpoint); got != tc.want {
			t.Fatalf("siblingEndpoint(%q) = %q, want %q", tc.apiEndpoint, got, tc.want)
		}
	}
}

func TestConfigHeartbeatURL(t *testing.T) {
	cfg := &Config{ApiEndpoint: "https://monitor.fascinated.cc/api/v1/servers/ingest"}
	if got := cfg.HeartbeatURL(); got != "https://monitor.fascinated.cc/api/v1/servers/heartbeat" {
		t.Fatalf("HeartbeatURL() = %q", got)
	}
}

func TestConfigUpdateURL(t *testing.T) {
	tests := []struct {
		apiEndpoint string
		want        string
	}{
		{"", defaultAgentUpdateEndpoint},
		{"https://monitor.fascinated.cc/api/v1/servers/ingest", "https://monitor.fascinated.cc/api/v1/agent/update"},
		{"https://api.example/api/v1/servers/ingest", "https://api.example/api/v1/agent/update"},
	}
	for _, tc := range tests {
		cfg := &Config{ApiEndpoint: tc.apiEndpoint}
		if got := cfg.UpdateURL(); got != tc.want {
			t.Fatalf("UpdateURL(%q) = %q, want %q", tc.apiEndpoint, got, tc.want)
		}
	}
}
