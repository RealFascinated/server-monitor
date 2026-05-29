package ingest

import (
	"fmt"
	"log/slog"
	"os"
	"strconv"
	"strings"

	"github.com/robfig/cron/v3"
	"gopkg.in/yaml.v3"
)

const (
	configFileEnvVar     = "MONITOR_CONFIG_FILE"
	ingestTokenEnvVar    = "MONITOR_INGEST_TOKEN"
	apiEndpointEnvVar    = "MONITOR_API_ENDPOINT"
	defaultConfigFile    = "config.yml"
	defaultPushSchedule  = "*/15 * * * * *"
)

type fileConfig struct {
	IngestToken  string `yaml:"ingest_token"`
	ApiEndpoint  string `yaml:"api_endpoint"`
	PushSchedule string `yaml:"push_schedule"`
	EnableDocker *bool  `yaml:"enable_docker"`
}

type Config struct {
	IngestToken  string
	ApiEndpoint  string
	PushSchedule string
	EnableDocker bool
}

func LoadConfig() (*Config, error) {
	path := os.Getenv(configFileEnvVar)
	if path == "" {
		path = defaultConfigFile
	}

	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config %q: %w", path, err)
	}

	var raw fileConfig
	if err := yaml.Unmarshal(data, &raw); err != nil {
		return nil, fmt.Errorf("parse config %q: %w", path, err)
	}

	schedule, err := resolvePushSchedule(raw)
	if err != nil {
		return nil, fmt.Errorf("parse config %q: %w", path, err)
	}

	config := &Config{
		PushSchedule: schedule,
		EnableDocker: true,
	}
	if raw.EnableDocker != nil {
		config.EnableDocker = *raw.EnableDocker
	}

	config.IngestToken = strings.TrimSpace(raw.IngestToken)
	config.ApiEndpoint = strings.TrimSpace(raw.ApiEndpoint)

	if token := strings.TrimSpace(os.Getenv(ingestTokenEnvVar)); token != "" {
		config.IngestToken = token
	}
	if endpoint := strings.TrimSpace(os.Getenv(apiEndpointEnvVar)); endpoint != "" {
		config.ApiEndpoint = endpoint
	}

	if config.ApiEndpoint == "" {
		return nil, fmt.Errorf("parse config %q: api_endpoint is required", path)
	}
	if config.IngestToken == "" {
		return nil, fmt.Errorf("parse config %q: ingest_token is required", path)
	}
	return config, nil
}

func resolvePushSchedule(raw fileConfig) (string, error) {
	if schedule := strings.TrimSpace(raw.PushSchedule); schedule != "" {
		return schedule, validatePushSchedule(schedule)
	}
	return defaultPushSchedule, nil
}

func validatePushSchedule(schedule string) error {
	parser := cron.NewParser(cron.Second | cron.Minute | cron.Hour | cron.Dom | cron.Month | cron.Dow | cron.DowOptional)
	_, err := parser.Parse(schedule)
	if err != nil {
		return fmt.Errorf("invalid push_schedule %q: %w", schedule, err)
	}
	return nil
}

func ParseLogLevel(value string) slog.Level {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "debug":
		return slog.LevelDebug
	case "warn", "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	default:
		if n, err := strconv.Atoi(value); err == nil {
			return slog.Level(n)
		}
		return slog.LevelInfo
	}
}
