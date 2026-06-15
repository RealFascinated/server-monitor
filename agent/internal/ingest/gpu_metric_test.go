package ingest

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestGPUMetricJSONIncludesZeroUsage(t *testing.T) {
	data, err := json.Marshal(GPUMetric{
		DeviceID:     "abc",
		Name:         "GPU",
		Vendor:       "nvidia",
		UsagePercent: 0,
	})
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(data), `"usagePercent":0`) {
		t.Fatalf("expected zero usage in JSON, got %s", data)
	}
	for _, field := range []string{
		`"memoryUsedBytes":0`,
		`"memoryTotalBytes":0`,
		`"temperatureCelsius":0`,
		`"powerWatts":0`,
	} {
		if !strings.Contains(string(data), field) {
			t.Fatalf("expected %s in JSON, got %s", field, data)
		}
	}
}

func TestGPUMetricJSONOmitsEncoderDecoderWhenUnset(t *testing.T) {
	data, err := json.Marshal(GPUMetric{
		DeviceID:     "abc",
		Name:         "Radeon RX 7900 XTX",
		Vendor:       "amd",
		UsagePercent: 12,
	})
	if err != nil {
		t.Fatal(err)
	}
	if strings.Contains(string(data), "encoderUsagePercent") {
		t.Fatalf("expected encoder omitted for AMD, got %s", data)
	}
	if strings.Contains(string(data), "decoderUsagePercent") {
		t.Fatalf("expected decoder omitted for AMD, got %s", data)
	}
}

func TestGPUMetricJSONIncludesZeroEncoderDecoderForNVIDIA(t *testing.T) {
	data, err := json.Marshal(GPUMetric{
		DeviceID:            "abc",
		Name:                "GPU",
		Vendor:              "nvidia",
		UsagePercent:        0,
		EncoderUsagePercent: FloatPtr(0),
		DecoderUsagePercent: FloatPtr(0),
	})
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(data), `"encoderUsagePercent":0`) {
		t.Fatalf("expected zero encoder usage in JSON, got %s", data)
	}
	if !strings.Contains(string(data), `"decoderUsagePercent":0`) {
		t.Fatalf("expected zero decoder usage in JSON, got %s", data)
	}
}
