package lhm

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestParseServerMetricsJSON(t *testing.T) {
	raw := `{
		"cpuTotalPercent": 11.5,
		"cpuPowerWatts": 42.5,
		"cores": [{"cpu":"0","usagePercent":40},{"cpu":"1","usagePercent":5}],
		"memory": {"used":100,"available":50,"total":150},
		"temperatures": [{"sensor":"cpu_die","celsius":46.9}],
		"gpus":[{"deviceId":"\\\\?\\PCI#VEN_10DE&DEV_2684","name":"NVIDIA GeForce RTX 4090","vendor":"nvidia","usagePercent":22.5,"encoderUsagePercent":80.0,"decoderUsagePercent":15.0,"memoryUsedBytes":1073741824,"memoryTotalBytes":25769803776,"temperatureCelsius":51.0,"powerWatts":180.0}]
	}`
	snap, err := ParseServerMetricsJSON([]byte(raw))
	if err != nil {
		t.Fatal(err)
	}
	if snap.CPUTotalPercent == nil || *snap.CPUTotalPercent != 11.5 {
		t.Fatalf("cpu total: %+v", snap.CPUTotalPercent)
	}
	if snap.CPUPowerWatts == nil || *snap.CPUPowerWatts != 42.5 {
		t.Fatalf("cpu power: %+v", snap.CPUPowerWatts)
	}
	if len(snap.Cores) != 2 || snap.Cores[0].CPU != "0" {
		t.Fatalf("cores: %+v", snap.Cores)
	}
	if !snap.Memory.Complete() {
		t.Fatal("expected complete memory")
	}

	var metrics ingest.ServerMetrics
	ApplyServerSnapshot(&metrics, snap)
	if metrics.CPUUsage != 11.5 {
		t.Fatalf("cpu usage: %v", metrics.CPUUsage)
	}
	if metrics.CPUPowerWatts != 42.5 {
		t.Fatalf("cpu power watts: %v", metrics.CPUPowerWatts)
	}
	if len(metrics.CPUCoreMetrics) != 2 {
		t.Fatalf("core metrics: %+v", metrics.CPUCoreMetrics)
	}
	if metrics.MemoryTotal != 150 {
		t.Fatalf("memory total: %v", metrics.MemoryTotal)
	}
	if len(metrics.TemperatureMetrics) != 1 || metrics.TemperatureMetrics[0].Sensor != "cpu_die" {
		t.Fatalf("temps: %+v", metrics.TemperatureMetrics)
	}
	if len(snap.GPUs) != 1 || snap.GPUs[0].Vendor != "nvidia" || snap.GPUs[0].UsagePercent != 22.5 {
		t.Fatalf("gpus: %+v", snap.GPUs)
	}
	if snap.GPUs[0].EncoderUsagePercent == nil || *snap.GPUs[0].EncoderUsagePercent != 80.0 {
		t.Fatalf("encoder: %+v", snap.GPUs[0].EncoderUsagePercent)
	}
	if snap.GPUs[0].DecoderUsagePercent == nil || *snap.GPUs[0].DecoderUsagePercent != 15.0 {
		t.Fatalf("decoder: %+v", snap.GPUs[0].DecoderUsagePercent)
	}
	if snap.GPUs[0].DeviceID != ingest.HashDeviceID(`\\?\PCI#VEN_10DE&DEV_2684`) {
		t.Fatalf("device id: got %q", snap.GPUs[0].DeviceID)
	}
}

func TestParseServerMetricsJSONMissingEncoderDecoder(t *testing.T) {
	raw := `{"gpus":[{"deviceId":"gpu-1","name":"GPU","vendor":"nvidia","usagePercent":10.0}]}`
	snap, err := ParseServerMetricsJSON([]byte(raw))
	if err != nil {
		t.Fatal(err)
	}
	if len(snap.GPUs) != 1 {
		t.Fatalf("gpus: %+v", snap.GPUs)
	}
	if snap.GPUs[0].EncoderUsagePercent != nil || snap.GPUs[0].DecoderUsagePercent != nil {
		t.Fatalf("encoder/decoder: %+v", snap.GPUs[0])
	}
}

func TestApplyServerSnapshotPartialMemory(t *testing.T) {
	used := int64(10)
	snap := ServerSnapshot{
		Memory: MemorySnapshot{Used: &used},
	}
	var metrics ingest.ServerMetrics
	metrics.MemoryTotal = 99
	ApplyServerSnapshot(&metrics, snap)
	if metrics.MemoryTotal != 99 {
		t.Fatalf("partial memory should not overwrite: %v", metrics.MemoryTotal)
	}
}
