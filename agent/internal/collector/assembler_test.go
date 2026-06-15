package collector

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/ingest"
)

func TestAssembleOverlaysSlowServerMetrics(t *testing.T) {
	t.Parallel()

	fast := FastSnapshot{
		Ready: true,
		ServerMetrics: ingest.ServerMetrics{
			CPUUsage: 42,
		},
		DiskMetrics: []ingest.DiskMetric{
			{DiskName: "/"},
		},
	}
	slow := SlowSnapshot{
		ServerMetrics: ingest.ServerMetrics{
			TemperatureMetrics: []ingest.TemperatureMetric{{Sensor: "cpu", Celsius: 55}},
		},
		Mounts: []disk.Mount{
			{Name: "/", UsedBytes: 10, TotalBytes: 100},
		},
		HasMounts: true,
		TCPConnectionMetrics: []ingest.TCPConnectionMetric{
			{State: "ESTABLISHED", Count: 3},
		},
	}

	result := Assemble(fast, slow)
	if result.ServerMetrics.CPUUsage != 42 {
		t.Fatalf("CPUUsage = %v, want 42", result.ServerMetrics.CPUUsage)
	}
	if len(result.ServerMetrics.TemperatureMetrics) != 1 {
		t.Fatalf("TemperatureMetrics = %+v", result.ServerMetrics.TemperatureMetrics)
	}
	if len(result.TCPConnectionMetrics) != 1 {
		t.Fatalf("TCPConnectionMetrics = %+v", result.TCPConnectionMetrics)
	}
	if result.DiskMetrics[0].UsedBytes != 10 {
		t.Fatalf("UsedBytes = %v, want 10", result.DiskMetrics[0].UsedBytes)
	}
}
