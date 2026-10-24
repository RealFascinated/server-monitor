package ingest

import (
	"testing"
	"time"
)

func TestResolveBufferConfigDefaults(t *testing.T) {
	buffer, err := resolveBufferConfig(fileConfig{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !buffer.Enabled || buffer.MaxAge != 6*time.Hour || buffer.MaxBytes != 16<<20 || buffer.MaxSamples != 0 {
		t.Fatalf("unexpected defaults: %+v", buffer)
	}
}

func TestResolveBufferConfigCustom(t *testing.T) {
	disabled := false
	buffer, err := resolveBufferConfig(fileConfig{
		EnablePushBuffer: &disabled,
		BufferMaxAge:     "30m",
		BufferMaxBytes:   "4MiB",
		BufferMaxSamples: "500",
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	want := BufferConfig{Enabled: false, MaxAge: 30 * time.Minute, MaxBytes: 4 << 20, MaxSamples: 500}
	if buffer != want {
		t.Fatalf("got %+v, want %+v", buffer, want)
	}
}

func TestResolveBufferConfigClampsAgeToWhatTheApiAccepts(t *testing.T) {
	buffer, err := resolveBufferConfig(fileConfig{BufferMaxAge: "48h"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if buffer.MaxAge != 6*time.Hour {
		t.Fatalf("max age = %v, want 6h", buffer.MaxAge)
	}
}

func TestResolveBufferConfigRejectsInvalidValues(t *testing.T) {
	for _, raw := range []fileConfig{
		{BufferMaxAge: "soon"},
		{BufferMaxAge: "-1h"},
		{BufferMaxBytes: "lots"},
		{BufferMaxBytes: "0"},
		{BufferMaxSamples: "-5"},
		{BufferMaxSamples: "ten"},
	} {
		if _, err := resolveBufferConfig(raw); err == nil {
			t.Fatalf("expected an error for %+v", raw)
		}
	}
}

func TestParseByteSize(t *testing.T) {
	tests := map[string]int{
		"1024": 1024, "1k": 1024, "2KiB": 2048, "16MiB": 16 << 20, "16mb": 16 << 20, "1g": 1 << 30,
	}
	for input, want := range tests {
		if got, err := parseByteSize(input); err != nil || got != want {
			t.Fatalf("parseByteSize(%q) = %d, %v; want %d", input, got, err, want)
		}
	}
	if _, err := parseByteSize("99999g"); err == nil {
		t.Fatal("expected overflow to be rejected")
	}
}

func TestBufferConfigFromEnvironment(t *testing.T) {
	t.Setenv(ConfigEnvVar("config_file"), "-")
	t.Setenv(ConfigEnvVar("ingest_token"), "token")
	t.Setenv(ConfigEnvVar("enable_push_buffer"), "false")
	t.Setenv(ConfigEnvVar("buffer_max_age"), "2h")
	t.Setenv(ConfigEnvVar("buffer_max_bytes"), "8MiB")

	config, err := LoadConfig()
	if err != nil {
		t.Fatalf("LoadConfig: %v", err)
	}
	want := BufferConfig{Enabled: false, MaxAge: 2 * time.Hour, MaxBytes: 8 << 20}
	if config.PushBuffer != want {
		t.Fatalf("got %+v, want %+v", config.PushBuffer, want)
	}
}
