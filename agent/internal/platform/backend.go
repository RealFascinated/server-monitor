package platform

import (
	"fascinated.cc/monitor/agent/internal/disk"
	"fascinated.cc/monitor/agent/internal/ingest"
)

type Options struct {
	HasZFS       bool
	EnableDocker bool
	EnableGPU    bool
}

type TickUpdate struct {
	Skip             bool
	Ready            bool
	ClockMHz         float64
	ServerMetrics    ingest.ServerMetrics
	InterfaceMetrics []ingest.InterfaceMetrics
	DiskMetrics      []ingest.DiskMetric
	ZfsArcMetrics    *ingest.ZFSArcMetrics
}

type SlowUpdate struct {
	ZfsPoolMetrics       []ingest.ZfsPoolMetric
	DockerContainers     []ingest.DockerContainerMetric
	GPUMetrics           []ingest.GPUMetric
	TCPConnectionMetrics []ingest.TCPConnectionMetric
	ServerMetrics        ingest.ServerMetrics
	Mounts               []disk.Mount
	HasMounts            bool
}

type Backend interface {
	Tick(ready bool) (TickUpdate, error)
	RefreshSlow() (SlowUpdate, error)
}
