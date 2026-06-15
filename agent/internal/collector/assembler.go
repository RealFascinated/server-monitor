package collector

import (
	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/ingest"
)

// FastSnapshot holds metrics updated on each fast tick.
type FastSnapshot struct {
	Ready            bool
	ClockMHz         float64
	ServerMetrics    ingest.ServerMetrics
	InterfaceMetrics []ingest.InterfaceMetrics
	DiskMetrics      []ingest.DiskMetric
	ZfsArcMetrics    *ingest.ZFSArcMetrics
}

// SlowSnapshot holds metrics updated on each slow refresh.
type SlowSnapshot struct {
	ZfsPoolMetrics       []ingest.ZfsPoolMetric
	DockerContainers     []ingest.DockerContainerMetric
	GPUMetrics           []ingest.GPUMetric
	TCPConnectionMetrics []ingest.TCPConnectionMetric
	ServerMetrics        ingest.ServerMetrics
	Mounts               []disk.Mount
	HasMounts            bool
}

func cloneFastSnapshot(f FastSnapshot) FastSnapshot {
	out := f
	if f.InterfaceMetrics != nil {
		out.InterfaceMetrics = append([]ingest.InterfaceMetrics(nil), f.InterfaceMetrics...)
	}
	if f.DiskMetrics != nil {
		out.DiskMetrics = append([]ingest.DiskMetric(nil), f.DiskMetrics...)
	}
	if f.ServerMetrics.CPUCoreMetrics != nil {
		out.ServerMetrics.CPUCoreMetrics = append([]ingest.CPUCoreMetric(nil), f.ServerMetrics.CPUCoreMetrics...)
	}
	if f.ZfsArcMetrics != nil {
		arc := *f.ZfsArcMetrics
		out.ZfsArcMetrics = &arc
	}
	return out
}

func cloneSlowSnapshot(s SlowSnapshot) SlowSnapshot {
	out := s
	if s.ZfsPoolMetrics != nil {
		out.ZfsPoolMetrics = append([]ingest.ZfsPoolMetric(nil), s.ZfsPoolMetrics...)
	}
	if s.DockerContainers != nil {
		out.DockerContainers = append([]ingest.DockerContainerMetric(nil), s.DockerContainers...)
	}
	if s.GPUMetrics != nil {
		out.GPUMetrics = append([]ingest.GPUMetric(nil), s.GPUMetrics...)
	}
	if s.TCPConnectionMetrics != nil {
		out.TCPConnectionMetrics = append([]ingest.TCPConnectionMetric(nil), s.TCPConnectionMetrics...)
	}
	if len(s.ServerMetrics.TemperatureMetrics) > 0 {
		out.ServerMetrics.TemperatureMetrics = append([]ingest.TemperatureMetric(nil), s.ServerMetrics.TemperatureMetrics...)
	}
	if len(s.ServerMetrics.CPUCoreMetrics) > 0 {
		out.ServerMetrics.CPUCoreMetrics = append([]ingest.CPUCoreMetric(nil), s.ServerMetrics.CPUCoreMetrics...)
	}
	if s.Mounts != nil {
		out.Mounts = append([]disk.Mount(nil), s.Mounts...)
	}
	return out
}

// Assemble builds a push-ready Result from fast and slow snapshots.
func Assemble(fast FastSnapshot, slow SlowSnapshot) Result {
	fast = cloneFastSnapshot(fast)
	slow = cloneSlowSnapshot(slow)

	result := Result{
		ServerMetrics:        fast.ServerMetrics,
		InterfaceMetrics:     fast.InterfaceMetrics,
		DiskMetrics:          fast.DiskMetrics,
		ZfsArcMetrics:        fast.ZfsArcMetrics,
		ZfsPoolMetrics:       slow.ZfsPoolMetrics,
		DockerContainers:     slow.DockerContainers,
		GPUMetrics:           slow.GPUMetrics,
		TCPConnectionMetrics: slow.TCPConnectionMetrics,
	}
	overlaySlowServerMetrics(&result.ServerMetrics, slow.ServerMetrics)
	if slow.HasMounts {
		disk.UpdateUsageFromMounts(result.DiskMetrics, slow.Mounts)
	}
	return result
}
