package ingest

import (
	"log/slog"
	"os"
	"path/filepath"
	"reflect"
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

func TestLoadConfigFromFile(t *testing.T) {
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
	unsetConfigEnv(t)

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

func TestLoadConfigEnvOverridesFile(t *testing.T) {
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
	t.Setenv(ConfigEnvVar("ingest_token"), "env-token")
	t.Setenv(ConfigEnvVar("api_endpoint"), "https://env.example/ingest")
	t.Setenv(ConfigEnvVar("push_schedule"), "*/20 * * * * *")
	t.Setenv(ConfigEnvVar("enable_docker"), "true")

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
	if cfg.PushSchedule != "*/20 * * * * *" {
		t.Fatalf("schedule: got %q", cfg.PushSchedule)
	}
	if !cfg.EnableDocker {
		t.Fatal("expected enable_docker true from env")
	}
}

func TestLoadConfigEnvOnly(t *testing.T) {
	dir := t.TempDir()
	t.Chdir(dir)

	t.Setenv(configFileEnvVar, "")
	unsetConfigEnv(t)
	t.Setenv(ConfigEnvVar("ingest_token"), "env-only-token")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("LoadConfig: %v", err)
	}
	if cfg.IngestToken != "env-only-token" {
		t.Fatalf("token: got %q", cfg.IngestToken)
	}
	if cfg.ApiEndpoint != defaultAPIEndpoint {
		t.Fatalf("endpoint: got %q, want default", cfg.ApiEndpoint)
	}
	if cfg.PushSchedule != defaultPushSchedule {
		t.Fatalf("schedule: got %q", cfg.PushSchedule)
	}
	if !cfg.EnableDocker {
		t.Fatal("expected enable_docker default true")
	}
}

func TestLoadConfigNoFileUsesEnv(t *testing.T) {
	dir := t.TempDir()
	t.Chdir(dir)

	unsetConfigEnv(t)
	os.Unsetenv(configFileEnvVar)
	t.Setenv(ConfigEnvVar("ingest_token"), "token")
	t.Setenv(ConfigEnvVar("api_endpoint"), "https://api.example/ingest")
	t.Setenv(ConfigEnvVar("enable_docker"), "false")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("LoadConfig: %v", err)
	}
	if cfg.EnableDocker {
		t.Fatal("expected enable_docker false")
	}
}

func TestLoadConfigRequiresToken(t *testing.T) {
	dir := t.TempDir()
	t.Chdir(dir)

	unsetConfigEnv(t)
	os.Unsetenv(configFileEnvVar)

	if _, err := LoadConfig(); err == nil {
		t.Fatal("expected error for missing ingest_token")
	}
}

func TestLoadConfigExplicitMissingFile(t *testing.T) {
	unsetConfigEnv(t)
	t.Setenv(configFileEnvVar, "/does/not/exist/config.yml")

	if _, err := LoadConfig(); err == nil {
		t.Fatal("expected error for missing explicit config file")
	}
}

func TestLoadConfigDisabledFile(t *testing.T) {
	t.Setenv(configFileEnvVar, "-")
	t.Setenv(ConfigEnvVar("ingest_token"), "token")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("LoadConfig: %v", err)
	}
	if cfg.IngestToken != "token" {
		t.Fatalf("token: got %q", cfg.IngestToken)
	}
}

func unsetConfigEnv(t *testing.T) {
	t.Helper()
	typ := reflect.TypeOf(fileConfig{})
	for i := 0; i < typ.NumField(); i++ {
		yamlKey := yamlTagKey(typ.Field(i).Tag.Get("yaml"))
		if yamlKey != "" {
			os.Unsetenv(ConfigEnvVar(yamlKey))
		}
	}
}
