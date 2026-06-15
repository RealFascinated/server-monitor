package ingest

import (
	"fmt"
	"log/slog"
	"os"
	"reflect"
	"strconv"
	"strings"
	"github.com/robfig/cron/v3"
	"gopkg.in/yaml.v3"
)

const (
	configEnvPrefix     = "MONITOR_"
	defaultConfigFile   = "config.yml"
	defaultPushSchedule = "*/5 * * * * *"
	defaultAPIEndpoint  = "https://monitor.fascinated.cc/api/v1/servers/ingest"
)

var configFileEnvVar = ConfigEnvVar("config_file")

type fileConfig struct {
	IngestToken  string `yaml:"ingest_token"`
	ApiEndpoint  string `yaml:"api_endpoint"`
	PushSchedule string `yaml:"push_schedule"`
	EnableDocker *bool  `yaml:"enable_docker"`
	EnableGPU    *bool  `yaml:"enable_gpu"`
}

type Config struct {
	IngestToken  string
	ApiEndpoint  string
	PushSchedule string
	EnableDocker bool
	EnableGPU    bool
}

// ConfigEnvVar returns the environment variable for a config YAML key (e.g. ingest_token -> MONITOR_INGEST_TOKEN).
func ConfigEnvVar(yamlKey string) string {
	return configEnvPrefix + strings.ToUpper(strings.ReplaceAll(yamlKey, "-", "_"))
}

func LoadConfig() (*Config, error) {
	return loadConfig(true)
}

// LoadConfigForPrint loads config for the print subcommand (ingest token optional).
func LoadConfigForPrint() (*Config, error) {
	return loadConfig(false)
}

func loadConfig(requireToken bool) (*Config, error) {
	raw, _, err := loadFileConfig()
	if err != nil {
		return nil, err
	}

	if err := applyEnvConfig(&raw); err != nil {
		return nil, err
	}

	schedule, err := resolvePushSchedule(raw)
	if err != nil {
		return nil, fmt.Errorf("config: %w", err)
	}

	config := &Config{
		IngestToken:  strings.TrimSpace(raw.IngestToken),
		ApiEndpoint:  strings.TrimSpace(raw.ApiEndpoint),
		PushSchedule: schedule,
		EnableDocker: true,
		EnableGPU:    true,
	}
	if raw.EnableDocker != nil {
		config.EnableDocker = *raw.EnableDocker
	}
	if raw.EnableGPU != nil {
		config.EnableGPU = *raw.EnableGPU
	}

	if config.ApiEndpoint == "" {
		config.ApiEndpoint = defaultAPIEndpoint
	}
	if requireToken && config.IngestToken == "" {
		return nil, fmt.Errorf(
			"config: ingest_token is required (set %s or ingest_token in config file)",
			ConfigEnvVar("ingest_token"),
		)
	}
	return config, nil
}

func loadFileConfig() (fileConfig, string, error) {
	path, optional := resolveConfigFilePath()
	if path == "" {
		return fileConfig{}, "", nil
	}

	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) && optional {
			return fileConfig{}, "", nil
		}
		return fileConfig{}, "", fmt.Errorf("read config %q: %w", path, err)
	}

	var raw fileConfig
	if err := yaml.Unmarshal(data, &raw); err != nil {
		return fileConfig{}, "", fmt.Errorf("parse config %q: %w", path, err)
	}
	return raw, path, nil
}

func resolveConfigFilePath() (path string, optional bool) {
	value, explicit := os.LookupEnv(configFileEnvVar)
	if explicit {
		value = strings.TrimSpace(value)
		if value == "" || value == "-" {
			return "", false
		}
		return value, false
	}
	return defaultConfigFile, true
}

func applyEnvConfig(raw *fileConfig) error {
	value := reflect.ValueOf(raw).Elem()
	typ := value.Type()

	for i := 0; i < typ.NumField(); i++ {
		field := typ.Field(i)
		yamlKey := yamlTagKey(field.Tag.Get("yaml"))
		if yamlKey == "" {
			continue
		}

		envValue, ok := os.LookupEnv(ConfigEnvVar(yamlKey))
		if !ok {
			continue
		}

		fieldValue := value.Field(i)
		switch fieldValue.Kind() {
		case reflect.String:
			if trimmed := strings.TrimSpace(envValue); trimmed != "" {
				fieldValue.SetString(trimmed)
			}
		case reflect.Ptr:
			if fieldValue.Type().Elem().Kind() != reflect.Bool {
				continue
			}
			if strings.TrimSpace(envValue) == "" {
				continue
			}
			parsed, err := parseBoolEnv(envValue)
			if err != nil {
				return fmt.Errorf("%s: %w", ConfigEnvVar(yamlKey), err)
			}
			fieldValue.Set(reflect.ValueOf(&parsed))
		}
	}
	return nil
}

func yamlTagKey(tag string) string {
	if tag == "" || tag == "-" {
		return ""
	}
	return strings.Split(tag, ",")[0]
}

func parseBoolEnv(value string) (bool, error) {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "1", "true", "yes", "on":
		return true, nil
	case "0", "false", "no", "off":
		return false, nil
	default:
		return false, fmt.Errorf("invalid boolean %q", value)
	}
}

func resolvePushSchedule(raw fileConfig) (string, error) {
	if schedule := strings.TrimSpace(raw.PushSchedule); schedule != "" {
		return schedule, validatePushSchedule(schedule)
	}
	return defaultPushSchedule, nil
}

func ValidatePushSchedule(schedule string) error {
	return validatePushSchedule(schedule)
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
