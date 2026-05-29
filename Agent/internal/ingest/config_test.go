package ingest

import (
	"log/slog"
	"os"
	"path/filepath"
	"testing"
)

func TestParseLogLevel(t *testing.T) {
	tests := []struct {
		input string
		want  slog.Level
	}{
		{"debug", slog.LevelDebug},
		{"WARN", slog.LevelWarn},
		{"warning", slog.LevelWarn},
		{"error", slog.LevelError},
		{"-4", slog.Level(-4)},
		{"", slog.LevelInfo},
		{"info", slog.LevelInfo},
		{"unknown", slog.LevelInfo},
	}
	for _, tc := range tests {
		if got := ParseLogLevel(tc.input); got != tc.want {
			t.Fatalf("ParseLogLevel(%q): got %v, want %v", tc.input, got, tc.want)
		}
	}
}

func TestResolvePushSchedule(t *testing.T) {
	schedule, err := resolvePushSchedule(fileConfig{})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if schedule != defaultPushSchedule {
		t.Fatalf("expected default %q, got %q", defaultPushSchedule, schedule)
	}

	schedule, err = resolvePushSchedule(fileConfig{PushSchedule: "*/30 * * * * *"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if schedule != "*/30 * * * * *" {
		t.Fatalf("expected custom schedule, got %q", schedule)
	}

	_, err = resolvePushSchedule(fileConfig{PushSchedule: "not-a-cron"})
	if err == nil {
		t.Fatal("expected invalid schedule error")
	}
}

func TestLoadConfig(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "config.yml")
	content := `
ingest_token: file-token
api_endpoint: https://api.example/ingest
push_schedule: "*/10 * * * * *"
enable_docker: false
`
	if err := os.WriteFile(path, []byte(content), 0o600); err != nil {
		t.Fatalf("write config: %v", err)
	}

	t.Setenv(configFileEnvVar, path)
	t.Setenv(ingestTokenEnvVar, "")
	t.Setenv(apiEndpointEnvVar, "")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("LoadConfig: %v", err)
	}
	if cfg.IngestToken != "file-token" {
		t.Fatalf("token: got %q", cfg.IngestToken)
	}
	if cfg.ApiEndpoint != "https://api.example/ingest" {
		t.Fatalf("endpoint: got %q", cfg.ApiEndpoint)
	}
	if cfg.PushSchedule != "*/10 * * * * *" {
		t.Fatalf("schedule: got %q", cfg.PushSchedule)
	}
	if cfg.EnableDocker {
		t.Fatal("expected enable_docker false")
	}
}

func TestLoadConfigEnvOverrides(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "config.yml")
	content := `
ingest_token: file-token
api_endpoint: https://api.example/ingest
`
	if err := os.WriteFile(path, []byte(content), 0o600); err != nil {
		t.Fatalf("write config: %v", err)
	}

	t.Setenv(configFileEnvVar, path)
	t.Setenv(ingestTokenEnvVar, "env-token")
	t.Setenv(apiEndpointEnvVar, "https://env.example/ingest")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("LoadConfig: %v", err)
	}
	if cfg.IngestToken != "env-token" {
		t.Fatalf("token: got %q", cfg.IngestToken)
	}
	if cfg.ApiEndpoint != "https://env.example/ingest" {
		t.Fatalf("endpoint: got %q", cfg.ApiEndpoint)
	}
}

func TestLoadConfigRequiresFields(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "config.yml")
	if err := os.WriteFile(path, []byte("ingest_token: tok\n"), 0o600); err != nil {
		t.Fatalf("write config: %v", err)
	}

	t.Setenv(configFileEnvVar, path)
	t.Setenv(apiEndpointEnvVar, "")

	if _, err := LoadConfig(); err == nil {
		t.Fatal("expected error for missing api_endpoint")
	}
}
