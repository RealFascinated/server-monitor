package ingest

import (
	"reflect"
	"testing"
)

func TestConfigEnvVar(t *testing.T) {
	tests := map[string]string{
		"ingest_token":  "MONITOR_INGEST_TOKEN",
		"api_endpoint":  "MONITOR_API_ENDPOINT",
		"push_schedule": "MONITOR_PUSH_SCHEDULE",
		"enable_docker": "MONITOR_ENABLE_DOCKER",
		"config_file":   "MONITOR_CONFIG_FILE",
	}
	for key, want := range tests {
		if got := ConfigEnvVar(key); got != want {
			t.Fatalf("ConfigEnvVar(%q) = %q, want %q", key, got, want)
		}
	}
}

func TestConfigEnvVarsMatchStruct(t *testing.T) {
	typ := reflect.TypeOf(fileConfig{})
	for i := 0; i < typ.NumField(); i++ {
		yamlKey := yamlTagKey(typ.Field(i).Tag.Get("yaml"))
		if yamlKey == "" {
			t.Fatalf("field %s missing yaml tag", typ.Field(i).Name)
		}
		if ConfigEnvVar(yamlKey) == configEnvPrefix {
			t.Fatalf("empty env var for %s", yamlKey)
		}
	}
}
