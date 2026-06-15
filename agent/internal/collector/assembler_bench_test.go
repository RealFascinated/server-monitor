package collector

import (
	"testing"

	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/ingest"
)

func BenchmarkAssemble(b *testing.B) {
	fast := FastSnapshot{
		Ready: true,
		ServerMetrics: ingest.ServerMetrics{
			CPUUsage: 42,
		},
		InterfaceMetrics: make([]ingest.InterfaceMetrics, 4),
		DiskMetrics:      make([]ingest.DiskMetric, 8),
	}
	slow := SlowSnapshot{
		DockerContainers: make([]ingest.DockerContainerMetric, 16),
		TCPConnectionMetrics: []ingest.TCPConnectionMetric{
			{State: "ESTABLISHED", Count: 10},
			{State: "TIME_WAIT", Count: 5},
		},
		Mounts:    make([]disk.Mount, 8),
		HasMounts: true,
	}

	b.ReportAllocs()
	for b.Loop() {
		_ = Assemble(fast, slow)
	}
}
