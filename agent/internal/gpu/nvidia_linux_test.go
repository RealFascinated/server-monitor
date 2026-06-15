//go:build linux

package gpu

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/ingest"
)

func gpuEncoder(m ingest.GPUMetric) (float64, bool) {
	if m.EncoderUsagePercent == nil {
		return 0, false
	}
	return *m.EncoderUsagePercent, true
}

func gpuDecoder(m ingest.GPUMetric) (float64, bool) {
	if m.DecoderUsagePercent == nil {
		return 0, false
	}
	return *m.DecoderUsagePercent, true
}

func TestParseNVIDIALine(t *testing.T) {
	metric, ok := parseNVIDIALine(`NVIDIA GeForce RTX 4090, GPU-11111111-2222-3333-4444-555555555555, 12, 3, 45, 1024, 24576, 125.50, 78, 22`)
	if !ok {
		t.Fatal("expected ok")
	}
	if metric.Vendor != "nvidia" {
		t.Fatalf("identity: %+v", metric)
	}
	wantID := ingest.HashDeviceID("GPU-11111111-2222-3333-4444-555555555555")
	if metric.DeviceID != wantID {
		t.Fatalf("device id: got %q want %q", metric.DeviceID, wantID)
	}
	if metric.UsagePercent != 12 || metric.MemoryUsedBytes != 1024*1024*1024 {
		t.Fatalf("usage/memory: %+v", metric)
	}
	if metric.TemperatureCelsius != 45 || metric.PowerWatts != 125.5 {
		t.Fatalf("temp/power: %+v", metric)
	}
	enc, ok := gpuEncoder(metric)
	if !ok || enc != 78 {
		t.Fatalf("encoder: %+v", metric.EncoderUsagePercent)
	}
	dec, ok := gpuDecoder(metric)
	if !ok || dec != 22 {
		t.Fatalf("decoder: %+v", metric.DecoderUsagePercent)
	}
}

func TestParseNVIDIALineQuotedName(t *testing.T) {
	metric, ok := parseNVIDIALine(`"GeForce RTX, 3080", GPU-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee, 0, 0, 40, 512, 10240, [N/A]`)
	if !ok {
		t.Fatal("expected ok")
	}
	if metric.Name != "GeForce RTX, 3080" {
		t.Fatalf("name: %q", metric.Name)
	}
	if metric.PowerWatts != 0 {
		t.Fatalf("power: %v", metric.PowerWatts)
	}
	if metric.EncoderUsagePercent != nil || metric.DecoderUsagePercent != nil {
		t.Fatalf("encoder/decoder: %+v", metric)
	}
}

func TestParseNVIDIALineZeroEncoderDecoder(t *testing.T) {
	metric, ok := parseNVIDIALine(`NVIDIA T400, GPU-00000000-0000-0000-0000-000000000001, 0, 0, 35, 0, 4096, [N/A], 0, 0`)
	if !ok {
		t.Fatal("expected ok")
	}
	enc, ok := gpuEncoder(metric)
	if !ok || enc != 0 {
		t.Fatalf("encoder: %+v", metric.EncoderUsagePercent)
	}
	dec, ok := gpuDecoder(metric)
	if !ok || dec != 0 {
		t.Fatalf("decoder: %+v", metric.DecoderUsagePercent)
	}
}
